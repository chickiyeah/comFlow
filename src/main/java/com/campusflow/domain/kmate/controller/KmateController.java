package com.campusflow.domain.kmate.controller;

import com.campusflow.domain.kmate.dto.*;
import com.campusflow.domain.kmate.service.KmateService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kmate")
@RequiredArgsConstructor
public class KmateController {

    private final KmateService kmateService;

    @PostMapping("/ask")
    public ApiResponse<KmateAskResponse> ask(@AuthenticationPrincipal String username,
                                             @Valid @RequestBody KmateAskRequest request) {
        return ApiResponse.ok(kmateService.ask(username, request.question()));
    }

    @GetMapping("/history")
    public ApiResponse<List<KmateHistoryResponse>> history(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(kmateService.history(username));
    }

    @PostMapping("/quiz/generate")
    public ApiResponse<List<KmateQuizQuestion>> generate(@Valid @RequestBody KmateQuizGenerateRequest request) {
        return ApiResponse.ok(kmateService.generateQuiz(request));
    }

    @PostMapping("/quiz/check")
    public ApiResponse<KmateQuizCheckResponse> check(@Valid @RequestBody KmateQuizCheckRequest request) {
        return ApiResponse.ok(kmateService.checkQuiz(request));
    }
}
