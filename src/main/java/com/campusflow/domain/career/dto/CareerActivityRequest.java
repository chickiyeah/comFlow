package com.campusflow.domain.career.dto;

import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CareerActivityRequest(
        @NotNull ActivityType type,
        @NotNull ActivityStatus status,
        @NotBlank String title,
        String organization,
        LocalDate targetDate,
        LocalDate completedDate,
        String score,
        String memo
) {}
