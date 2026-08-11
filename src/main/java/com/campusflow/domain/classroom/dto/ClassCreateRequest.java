package com.campusflow.domain.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassCreateRequest(
        @NotBlank(message = "클래스 이름은 필수입니다.")
        @Size(max = 100, message = "클래스 이름은 100자 이하여야 합니다.")
        String name,

        @Size(max = 100, message = "과목명은 100자 이하여야 합니다.")
        String subject,

        @Size(max = 255, message = "설명은 255자 이하여야 합니다.")
        String description
) {
}
