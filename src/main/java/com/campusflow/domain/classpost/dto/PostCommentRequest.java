package com.campusflow.domain.classpost.dto;

import jakarta.validation.constraints.NotBlank;

public record PostCommentRequest(
        @NotBlank(message = "내용은 필수입니다.")
        String body
) {
}
