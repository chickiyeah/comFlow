package com.campusflow.domain.ai.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 시맨틱 답변 캐시(일반 Q&A 전용). 의미적으로 유사한 과거 질문이면 AI를 호출하지 않고 저장 답변을 반환한다.
 *
 * <p>동작:
 * <ol>
 *   <li>사용자 프롬프트에 최신성 키워드(새로/최신/최신화 등)가 있으면 캐시 우회 → AI 호출(+ 최신 답변으로 갱신).</li>
 *   <li>없으면 질문을 임베딩해 ChromaDB에서 최근접 검색 → 유사도 임계값 이내면 저장 답변 즉시 반환.</li>
 *   <li>미스면 AI 호출 후 write-through로 비동기 저장.</li>
 * </ol>
 *
 * <p><b>best-effort</b>: 임베딩/ChromaDB 실패는 전부 무시하고 AI로 통과한다 — 캐시가 요청을 절대 깨지 않는다.
 * namespace로 도메인(komjeong·kmate 등)을 격리해 교차 오염을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    @Value("${semantic-cache.enabled:true}")               private boolean enabled;
    @Value("${semantic-cache.similarity-threshold:0.93}")  private double similarityThreshold;
    @Value("${semantic-cache.ttl-days:30}")                private long ttlDays;
    @Value("${semantic-cache.freshness-keywords:새로,새로운,최신,최신화,업데이트,요즘,올해,지금,현재,오늘,최근}")
    private String freshnessKeywordsCsv;

    private final GatewayEmbeddingClient embeddingClient;
    private final ChromaCacheStore store;

    /**
     * 유사 질문이면 캐시 답변, 아니면 {@code aiCall}로 계산 후 캐시에 적재.
     *
     * @param namespace 도메인 격리 키(예: "komjeong", "kmate-tutor")
     * @param question  사용자 질문
     * @param aiCall    실제 답변 계산(캐시 미스/우회 시 호출)
     */
    public String getOrCompute(String namespace, String question, Supplier<String> aiCall) {
        if (!enabled || question == null || question.isBlank()) {
            return aiCall.get();
        }

        boolean fresh = hasFreshnessKeyword(question);
        float[] emb = embeddingClient.embed(question);

        if (emb != null && !fresh) {
            double maxDistance = 1.0 - similarityThreshold;
            long minTs = Instant.now().getEpochSecond() - ttlDays * 86_400L;
            ChromaCacheStore.Hit hit = store.lookup(emb, namespace, maxDistance, minTs);
            if (hit != null) {
                log.info("[SemanticCache] HIT ns={} dist={} q='{}'",
                        namespace, String.format("%.4f", hit.distance()), preview(question));
                return hit.answer();
            }
        }

        String answer = aiCall.get();
        if (emb != null && answer != null && !answer.isBlank()) {
            final float[] e = emb;
            final String a = answer;
            final long ts = Instant.now().getEpochSecond();
            CompletableFuture.runAsync(() -> store.store(e, namespace, question, a, ts));
        }
        return answer;
    }

    private boolean hasFreshnessKeyword(String q) {
        String lower = q.toLowerCase();
        for (String kw : freshnessKeywordsCsv.split(",")) {
            String k = kw.trim().toLowerCase();
            if (!k.isEmpty() && lower.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private String preview(String q) {
        return q.length() > 40 ? q.substring(0, 40) + "…" : q;
    }
}
