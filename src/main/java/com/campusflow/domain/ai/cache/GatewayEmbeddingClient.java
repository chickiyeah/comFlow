package com.campusflow.domain.ai.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 게이트웨이 {@code embed} 역할(nomic-embed-text) 호출로 텍스트 임베딩을 얻는다.
 * 시맨틱 캐시 전용. best-effort — 실패/타임아웃 시 {@code null}(호출측이 캐시를 건너뛰고 AI로 진행).
 * 게이트웨이 base-url/가상키는 {@code litellm.*}(campusflow 키, embed allowlist 포함)를 재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayEmbeddingClient {

    @Value("${litellm.base-url:}")                  private String baseUrl;
    @Value("${litellm.api-key:}")                   private String apiKey;
    @Value("${semantic-cache.embed-model:embed}")   private String model;
    @Value("${semantic-cache.embed-timeout-ms:4000}") private int timeoutMs;

    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    /** 텍스트 임베딩. 실패/타임아웃/미설정 시 null. */
    public float[] embed(String text) {
        if (!isEnabled() || text == null || text.isBlank()) {
            return null;
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);

            String raw = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build()
                    .post().uri("/v1/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(Map.of("model", model, "input", text))
                    .retrieve().body(String.class);

            JsonNode arr = objectMapper.readTree(raw).path("data").path(0).path("embedding");
            if (!arr.isArray() || arr.isEmpty()) {
                return null;
            }
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                v[i] = (float) arr.get(i).asDouble();
            }
            return v;
        } catch (Exception e) {
            log.debug("[SemanticCache] embed 실패(무시): {}", e.getMessage());
            return null;
        }
    }
}
