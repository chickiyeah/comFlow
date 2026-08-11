package com.campusflow.domain.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseCommentRequest(
        @NotBlank String content,
        boolean question,
        Long parentId
) {}
