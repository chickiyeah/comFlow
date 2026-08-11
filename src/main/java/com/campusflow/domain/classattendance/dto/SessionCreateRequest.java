package com.campusflow.domain.classattendance.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SessionCreateRequest(
        @NotBlank(message = "세션 제목은 필수입니다.")
        String title,

        LocalDate date
) {
}
