package com.campusflow.domain.assignment.dto;

import jakarta.validation.constraints.NotNull;

public record DraftRequest(
        @NotNull(message = "draft 값은 필수입니다.")
        Boolean draft
) {
}
