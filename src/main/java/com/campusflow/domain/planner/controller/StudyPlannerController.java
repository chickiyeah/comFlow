package com.campusflow.domain.planner.controller;

import com.campusflow.domain.planner.dto.GradePredictRequest;
import com.campusflow.domain.planner.dto.StudyPlanRequest;
import com.campusflow.domain.planner.service.StudyPlannerService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
public class StudyPlannerController {

    private final StudyPlannerService plannerService;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(
            @AuthenticationPrincipal String username,
            @RequestBody StudyPlanRequest request) {
        return ApiResponse.ok(plannerService.generatePlan(username, request));
    }

    @PostMapping("/predict-grade")
    public ApiResponse<Map<String, Object>> predictGrade(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody GradePredictRequest request) {
        return ApiResponse.ok(plannerService.predictGrade(username, request));
    }
}
