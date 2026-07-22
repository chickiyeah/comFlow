package com.campusflow.domain.progress.controller;

import com.campusflow.domain.progress.dto.ProgressSummaryResponse;
import com.campusflow.domain.progress.service.ProgressService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/summary")
    public ApiResponse<ProgressSummaryResponse> summary(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(progressService.summary(username));
    }
}
