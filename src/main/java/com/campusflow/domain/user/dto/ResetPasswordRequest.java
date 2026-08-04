package com.campusflow.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email(message = "올바른 이메일 형식을 입력해주세요.") String email,
        @NotBlank String code,
        @NotBlank @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다.") String newPassword
) {}
