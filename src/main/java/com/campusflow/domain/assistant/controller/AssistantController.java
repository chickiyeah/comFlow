package com.campusflow.domain.assistant.controller;

import com.campusflow.domain.assistant.dto.AssistantResponse;
import com.campusflow.domain.assistant.dto.CoverLetterRequest;
import com.campusflow.domain.assistant.dto.CoverLetterResponse;
import com.campusflow.domain.assistant.service.AssistantService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping("/analyze")
    public ApiResponse<AssistantResponse> analyze(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(assistantService.analyze(username));
    }

    @PostMapping("/cover-letter")
    public ApiResponse<CoverLetterResponse> coverLetter(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CoverLetterRequest request) {
        return ApiResponse.ok(assistantService.generateCoverLetter(username, request));
    }
}
