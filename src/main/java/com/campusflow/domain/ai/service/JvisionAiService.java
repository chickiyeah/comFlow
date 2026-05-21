package com.campusflow.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * jvision.ai — 전주비전대학교 자체 vLLM 서버 (OpenAI 호환 포맷)
 * https://jvision.ai/api/chat/completions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JvisionAiService {

    private static final String BASE_URL = "https://jvision.ai";
    private static final String MODEL    = "vLLM.GPT-V-base";

    @Value("${jvision.ai.token:}")
    private String token;

    private final ObjectMapper objectMapper;

    public String ask(String systemPrompt, String userMessage) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("jvision.ai 토큰 미설정");
        }

        Map<String, Object> body = Map.of(
                "model",    MODEL,
                "stream",   false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userMessage)
                )
        );

        String raw = RestClient.create(BASE_URL).post()
                .uri("/api/chat/completions")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",  "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("jvision.ai 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}
