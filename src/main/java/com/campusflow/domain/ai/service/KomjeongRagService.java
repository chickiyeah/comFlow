package com.campusflow.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 컴정이(komjeong) RAG — AI컴퓨터학과 내부 자료 기반 RAG 어시스턴트.
 * BM25+벡터 하이브리드로 ~35,000 학과 청크를 검색해 근거 있는 답변을 반환한다.
 * OpenAI 호환 엔드포인트(/v1/chat/completions, model=komjeong) 사용.
 * 마크다운 산문을 반환하므로 호출 측에서 [학과 자료] 컨텍스트로 활용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KomjeongRagService {

    private static final String MODEL = "komjeong";

    @Value("${komjeong.base-url:http://10.8.0.17:8000}")
    private String baseUrl;

    private final ObjectMapper objectMapper;

    public String retrieveContext(String query) {
        Map<String, Object> body = Map.of(
                "model",    MODEL,
                "stream",   false,
                "messages", List.of(Map.of("role", "user", "content", query))
        );

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);

        try {
            String raw = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build()
                    .post()
                    .uri("/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("choices").get(0).path("message").path("content").asText("");
            // RAG 미스 시 "수집 중" 안내문/짧은 응답 → 컨텍스트 없음으로 처리 (호출 측이 폴백)
            if (content.contains("수집 중") || content.contains("자료에 해당 정보가 없") || content.strip().length() < 200) {
                log.info("[komjeong] RAG 미스(placeholder/짧음, {}자) — 빈 컨텍스트 반환", content.strip().length());
                return "";
            }
            return content;
        } catch (Exception e) {
            log.warn("[komjeong] RAG 호출 실패 ({}): {} — 빈 컨텍스트로 진행",
                    e.getClass().getSimpleName(), e.getMessage());
            return "";
        }
    }
}
