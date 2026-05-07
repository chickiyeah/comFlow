package com.campusflow.domain.award.dto;

import com.campusflow.domain.award.entity.AwardLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AwardRequest(
        @NotBlank(message = "수상명을 입력해주세요.") String title,
        @NotBlank(message = "주관 기관을 입력해주세요.") String organization,
        @NotNull(message = "수상 등급을 선택해주세요.") AwardLevel level,
        @NotNull(message = "수상 날짜를 입력해주세요.") LocalDate awardDate,
        String description
) {}
