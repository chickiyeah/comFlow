package com.campusflow.domain.user.dto;

import com.campusflow.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank(message = "학번/교번을 입력해주세요.") String username,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password,
        @NotBlank(message = "이름을 입력해주세요.") String name,
        @NotNull(message = "권한을 선택해주세요.") Role role
) {}
