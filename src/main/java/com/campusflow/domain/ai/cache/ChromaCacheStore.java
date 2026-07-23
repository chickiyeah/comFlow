package com.campusflow.domain.ai.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChromaDB v2 시맨틱 캐시 저장소({@code 10.8.0.17:8001}, cosine 컬렉션). 시맨틱 캐시 전용.
 * best-effort — 모든 예외는 잡아 로그만 남기고 무해 처리한다(캐시는 요청을 깨지 않는다).
 * 컬렉션 id는 최초 사용 시 {@code get_or_create}로 lazy 확보 후 캐싱한다.
 */
@Slf4j
@Component
public class ChromaCacheStore {

    @Value("${chroma.base-url:http://10.8.0.17:8001}") private String baseUrl;
    @Value("${chroma.collection:campusflow_qa_cache}") private String collection;
    @Value("${chroma.tenant:default_tenant}")          private String tenant;
    @Value("${chroma.database:default_database}")      private String database;

    private final ObjectMapper objectMapper;
    private volatile String collectionId;

    public ChromaCacheStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record Hit(String answer, double distance) {}

    private RestClient client() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(5000);
        return RestClient.builder().requestFactory(f).baseUrl(baseUrl).build();
    }

    private String collectionsBase() {
        return "/api/v2/tenants/" + tenant + "/databases/" + database + "/collections";
    }

    /** 컬렉션 id lazy 확보(get_or_create, cosine). 실패 시 null. */
    private String resolveCollectionId() {
        String id = collectionId;
        if (id != null) {
            return id;
        }
        synchronized (this) {
            if (collectionId != null) {
                return collectionId;
            }
            try {
                String raw = client().post().uri(collectionsBase())
                        .header("Content-Type", "application/json")
                        .body(Map.of(
                                "name", collection,
                                "configuration", Map.of("hnsw", Map.of("space", "cosine")),
                                "get_or_create", true))
                        .retrieve().body(String.class);
                String cid = objectMapper.readTree(raw).path("id").asText(null);
                collectionId = (cid == null || cid.isBlank()) ? null : cid;
                return collectionId;
            } catch (Exception e) {
                log.warn("[SemanticCache] chroma 컬렉션 확보 실패(무시): {}", e.getMessage());
                return null;
            }
        }
    }

    /**
     * 최근접 조회. 거리 오름차순 결과에서 {@code ns} 일치 && {@code ts >= minTs} && {@code 거리 <= maxDistance}인
     * 첫 항목을 반환. 없으면 null.
     */
    public Hit lookup(float[] emb, String ns, double maxDistance, long minTs) {
        String id = resolveCollectionId();
        if (id == null) {
            return null;
        }
        try {
            String raw = client().post().uri(collectionsBase() + "/" + id + "/query")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "query_embeddings", List.of(toList(emb)),
                            "n_results", 5,
                            "include", List.of("distances", "metadatas")))
                    .retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode dists = root.path("distances").path(0);
            JsonNode metas = root.path("metadatas").path(0);
            for (int i = 0; i < dists.size(); i++) {
                double dist = dists.get(i).asDouble(9.0);
                if (dist > maxDistance) {
                    break; // 오름차순 → 이후는 모두 더 멂
                }
                JsonNode m = metas.get(i);
                if (m == null || m.isNull()) {
                    continue;
                }
                if (!ns.equals(m.path("ns").asText(""))) {
                    continue;
                }
                if (m.path("ts").asLong(0) < minTs) {
                    continue;
                }
                String ans = m.path("answer").asText("");
                if (!ans.isBlank()) {
                    return new Hit(ans, dist);
                }
            }
        } catch (Exception e) {
            log.debug("[SemanticCache] chroma 조회 실패(무시): {}", e.getMessage());
        }
        return null;
    }

    /** 캐시 저장(add). best-effort. */
    public void store(float[] emb, String ns, String question, String answer, long ts) {
        String id = resolveCollectionId();
        if (id == null) {
            return;
        }
        try {
            client().post().uri(collectionsBase() + "/" + id + "/add")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "ids", List.of(UUID.randomUUID().toString()),
                            "embeddings", List.of(toList(emb)),
                            "documents", List.of(question),
                            "metadatas", List.of(Map.of("ns", ns, "answer", answer, "ts", ts))))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.debug("[SemanticCache] chroma 저장 실패(무시): {}", e.getMessage());
        }
    }

    private List<Float> toList(float[] a) {
        List<Float> l = new ArrayList<>(a.length);
        for (float v : a) {
            l.add(v);
        }
        return l;
    }
}
