package com.campusflow.domain.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AcademicUpdateRequest(
        @Min(1) @Max(4) int grade,
        @Min(1) @Max(2) int semester
) {}
