package com.campusflow.domain.gamification.controller;

import com.campusflow.domain.gamification.dto.GamificationResponse;
import com.campusflow.domain.gamification.service.GamificationService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/me")
    public ApiResponse<GamificationResponse> me(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(gamificationService.getMyStatus(username));
    }
}
