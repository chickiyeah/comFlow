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
 * Google Gemini API — 최후 폴백
 * gemini-1.5-flash (무료 티어, 이미지 지원)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String MODEL    = "gemini-1.5-flash";

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    public String ask(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API 키 미설정");
        }

        // Gemini는 system/user 역할 분리 없이 단일 텍스트로 전달
        String combined = systemPrompt + "\n\n" + userMessage;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", combined)))
                )
        );

        String raw = RestClient.create(BASE_URL).post()
                .uri("/" + MODEL + ":generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("");
        } catch (Exception e) {
            log.warn("Gemini 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }
}
