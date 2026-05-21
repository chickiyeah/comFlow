package com.campusflow.domain.study.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StudyGroupRequest(
        @NotBlank String name,
        String subject,
        String description,
        @Min(2) @Max(10) int maxMembers
) {}
