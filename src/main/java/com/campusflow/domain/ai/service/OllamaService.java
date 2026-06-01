package com.campusflow.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ollama 로컬 서버 — 라운드로빈 + 순차 폴백
 *
 * ⚠️ 반드시 native /api/chat 엔드포인트 사용.
 *    OpenAI 호환 /v1/chat/completions + think:false → content 빈 문자열 버그.
 *    native 응답 구조: message.content (choices 아님)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private static final String MODEL = "qwen3:8b";
    private static final String PATH  = "/api/chat";

    @Value("${ollama.servers:http://10.8.0.30:11434,http://10.8.0.31:11434,http://10.8.0.32:11434,http://10.8.0.33:11434,http://10.8.0.34:11434}")
    private String serversConfig;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ObjectMapper objectMapper;

    public String ask(String systemPrompt, String userMessage) {
        List<String> servers = Arrays.stream(serversConfig.split(","))
                .map(String::trim).toList();

        int start = counter.getAndIncrement() % servers.size();

        for (int i = 0; i < servers.size(); i++) {
            String server = servers.get((start + i) % servers.size());
            try {
                log.info("[Ollama] trying {}", server);
                String result = callOllama(server, systemPrompt, userMessage);
                if (!result.isBlank()) {
                    log.info("[Ollama] success: {}", server);
                    return result;
                }
                log.warn("[Ollama] blank response from {}", server);
            } catch (Exception e) {
                log.warn("[Ollama] FAIL {} : {} - {}", server, e.getClass().getSimpleName(), e.getMessage());
            }
        }
        throw new RuntimeException("All Ollama servers failed");
    }

    private String callOllama(String serverUrl, String systemPrompt, String userMessage) throws Exception {
        Map<String, Object> body = Map.of(
                "model",    MODEL,
                "stream",   false,
                "think",    false,   // Qwen3 thinking 비활성화 (속도 우선)
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userMessage)
                )
        );

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 연결 타임아웃 5초 (다운 서버 빠른 건너뛰기)
        factory.setReadTimeout(180000);    // 응답 타임아웃 3분 (LLM 생성 대기)

        String raw = RestClient.builder().requestFactory(factory).baseUrl(serverUrl).build()
                .post()
                .uri(PATH)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(raw);
        return root.path("message").path("content").asText("");
    }
}
