package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.JobMarketStats;
import com.campusflow.domain.career.service.EmploymentStatService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerStatController {

    private final EmploymentStatService employmentStatService;

    /**
     * 개인별 취업 통계 — 희망 직무 기반 예상 연봉·요구 학력/자격.
     * jobTitle 미지정 시 프로필에 저장된 희망 직무를 사용.
     */
    @GetMapping("/statistics")
    public ApiResponse<JobMarketStats> getStatistics(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String jobTitle) {
        return ApiResponse.ok(employmentStatService.getStatistics(username, jobTitle));
    }
}
