package com.campusflow.domain.ai.service;

import com.campusflow.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClaudeApiService {

    private final AiProperties aiProperties;

    private static final String BASE_URL = "https://api.anthropic.com";

    public String ask(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getClaude().getModel(),
                "max_tokens", 2048,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = RestClient.create(BASE_URL).post()
                .uri("/v1/messages")
                .header("x-api-key", aiProperties.getClaude().getApiKey())
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return "";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) return "";

        return (String) content.get(0).get("text");
    }
}
