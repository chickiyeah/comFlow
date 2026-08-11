package com.campusflow.domain.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinClassRequest(
        @NotBlank(message = "참여 코드는 필수입니다.")
        String code
) {
}
