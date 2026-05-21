package com.campusflow.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CourseReviewRequest(
        @NotBlank String subjectName,
        String professor,
        int year,
        int semester,
        @Min(1) @Max(5) int rating,
        String content,
        boolean anonymous
) {}
