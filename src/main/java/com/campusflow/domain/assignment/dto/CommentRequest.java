package com.campusflow.domain.assignment.dto;

import jakarta.validation.constraints.NotBlank;

/** studentId는 교사가 특정 학생 스레드에 쓸 때만 사용(학생은 무시하고 본인 스레드). */
public record CommentRequest(
        @NotBlank(message = "내용은 필수입니다.")
        String body,

        Long studentId
) {
}
