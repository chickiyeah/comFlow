package com.campusflow.global.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 컴정이(10.8.0.17:8000) API 프록시
 * 학식 조회, 학과 공지 등 학과 자료 기반 질의에 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KomjeongService {

    @Value("${komjeong.url:http://10.8.0.17:8000}")
    private String baseUrl;

    private final ObjectMapper objectMapper;

    @Cacheable(value = "komjeongMeal", key = "#date")
    public String getMeal(String date) {
        String question = date.equals("today") ? "오늘 학식 알려줘" : "내일 학식 알려줘";
        return query(question);
    }

    public String query(String question) {
        return queryWithSession(question, "campusflow_proxy_" + System.currentTimeMillis());
    }

    public String queryWithSession(String question, String sessionKey) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "message", question,
                    "session_id", sessionKey
            ));

            // RestClient body=null 버그 회피 — HttpClient + HTTP/1.1 강제 (uvicorn HTTP/2 미지원)
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json, text/plain, */*")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> resp = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build()
                    .send(req, HttpResponse.BodyHandlers.ofByteArray());

            String raw = resp.body() == null ? "" : new String(resp.body(), StandardCharsets.UTF_8);
            log.info("[컴정이] status={} chars={}", resp.statusCode(), raw.length());
            return parseResponse(raw);
        } catch (Exception e) {
            log.warn("컴정이 API 호출 실패: {}", e.getMessage());
            return "";
        }
    }

    private String parseResponse(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // SSE 포맷 (data: ... 줄) 처리
        if (raw.contains("data: ")) {
            return Arrays.stream(raw.split("\n"))
                    .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                    .map(line -> line.substring(6))
                    .collect(Collectors.joining(""))
                    .trim();
        }

        // JSON 포맷 처리
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.has("answer")) return node.get("answer").asText();
            if (node.has("response")) return node.get("response").asText();
        } catch (Exception ignored) {}

        return raw.trim();
    }
}
