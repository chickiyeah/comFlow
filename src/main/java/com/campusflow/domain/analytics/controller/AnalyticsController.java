package com.campusflow.domain.analytics.controller;

import com.campusflow.domain.analytics.dto.GradeTrendResponse;
import com.campusflow.domain.analytics.service.AnalyticsService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/grades")
    public ApiResponse<GradeTrendResponse> getGradeTrend(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(analyticsService.getGradeTrend(username));
    }

    @GetMapping("/attendance")
    public ApiResponse<Map<String, Object>> getAttendanceSummary(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(analyticsService.getAttendanceSummary(username));
    }
}
