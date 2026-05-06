package com.campusflow.domain.ai.service;

import com.campusflow.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final AiProperties aiProperties;

    private static final String BASE_URL = "https://api.openai.com";

    public String ask(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getOpenai().getModel(),
                "max_tokens", 2048,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = RestClient.create(BASE_URL).post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return "";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";

        @SuppressWarnings("unchecked")
        Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
        return message != null ? message.getOrDefault("content", "") : "";
    }
}
