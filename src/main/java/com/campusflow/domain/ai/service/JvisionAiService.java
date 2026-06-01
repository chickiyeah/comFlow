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
            JsonNode message = root.path("choices").get(0).path("message");
            String content = message.path("content").asText("");
            String reasoning = message.path("reasoning").asText("");
            // vLLM.GPT-V-base는 reasoning 모델: 최종 답변이 content가 비거나 짧을 때 reasoning에 담김.
            // JSON 구조화 호출에 대비해 '{' 포함 여부로 실제 답변이 있는 필드를 선택한다.
            if (content.isBlank() || (!content.contains("{") && reasoning.contains("{"))) {
                return reasoning.isBlank() ? content : reasoning;
            }
            return content;
        } catch (Exception e) {
            log.warn("jvision.ai 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}
