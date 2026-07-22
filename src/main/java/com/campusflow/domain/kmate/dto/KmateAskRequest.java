package com.campusflow.domain.kmate.dto;

import jakarta.validation.constraints.NotBlank;

public record KmateAskRequest(
        @NotBlank(message = "질문은 필수입니다.")
        String question
) {
}
