package com.campusflow.global.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 컴정이(10.8.0.2:8000) API 프록시
 * 학식 조회, 학과 공지 등 학과 자료 기반 질의에 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KomjeongService {

    @Value("${komjeong.url:http://10.8.0.2:8000}")
    private String baseUrl;

    private final ObjectMapper objectMapper;

    @Cacheable(value = "komjeongMeal", key = "#date")
    public String getMeal(String date) {
        String question = date.equals("today") ? "오늘 학식 알려줘" : "내일 학식 알려줘";
        return query(question);
    }

    public String query(String question) {
        try {
            String raw = RestClient.create(baseUrl).post()
                    .uri("/chat")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "query", question,
                            "session_id", "campusflow_proxy_" + System.currentTimeMillis()
                    ))
                    .retrieve()
                    .body(String.class);

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
