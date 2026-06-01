package com.campusflow.domain.komjeong.controller;

import com.campusflow.global.response.ApiResponse;
import com.campusflow.global.service.KomjeongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 컴정이 일반 질의 프록시 — 랜딩 페이지 검색창에서 사용 (비로그인 허용)
 */
@RestController
@RequestMapping("/api/komjeong")
@RequiredArgsConstructor
public class KomjeongChatController {

    private final KomjeongService komjeongService;

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("query", "").trim();
        String sessionId = body.getOrDefault("sessionId", "").trim();
        if (question.isEmpty()) {
            return ApiResponse.ok(Map.of("answer", ""));
        }
        String answer = sessionId.isEmpty()
                ? komjeongService.query(question)
                : komjeongService.queryWithSession(question, sessionId);
        return ApiResponse.ok(Map.of("answer", answer));
    }
}
