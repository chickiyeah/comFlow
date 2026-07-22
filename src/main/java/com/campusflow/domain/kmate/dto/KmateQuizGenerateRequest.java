package com.campusflow.domain.kmate.dto;

import jakarta.validation.constraints.NotBlank;

public record KmateQuizGenerateRequest(
        @NotBlank(message = "주제는 필수입니다.")
        String topic,

        Integer count,
        String language
) {
}
