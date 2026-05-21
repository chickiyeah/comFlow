package com.campusflow.domain.planner.dto;

import jakarta.validation.constraints.NotBlank;

public record GradePredictRequest(
        @NotBlank String subjectName,
        double attendanceRate,
        double midtermScore,
        double midtermWeight,
        double finalWeight
) {}
