package com.campusflow.domain.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull(message = "점수는 필수입니다.")
        @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
        Integer grade,

        String feedback
) {
}
