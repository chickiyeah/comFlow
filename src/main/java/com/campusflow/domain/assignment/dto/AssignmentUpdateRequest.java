package com.campusflow.domain.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssignmentUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        String instructions,
        LocalDateTime dueDate,
        Integer points
) {
}
