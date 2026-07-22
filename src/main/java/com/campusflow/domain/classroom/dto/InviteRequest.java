package com.campusflow.domain.classroom.dto;

import com.campusflow.domain.classroom.entity.ClassRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일로 기존 사용자를 클래스에 초대. role 미지정 시 STUDENT로 참여.
 */
public record InviteRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        ClassRole role
) {
}
