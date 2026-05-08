package com.campusflow.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.") String email
) {}
