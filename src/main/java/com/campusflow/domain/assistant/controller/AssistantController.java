package com.campusflow.domain.assistant.controller;

import com.campusflow.domain.assistant.dto.AssistantResponse;
import com.campusflow.domain.assistant.service.AssistantService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping("/analyze")
    public ApiResponse<AssistantResponse> analyze(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(assistantService.analyze(user.getUsername()));
    }
}
