package com.campusflow.domain.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseRequest(
        @NotBlank String title,
        String description,
        @NotBlank String videoUrl,
        String thumbnailUrl,
        String category,
        Boolean active
) {}
