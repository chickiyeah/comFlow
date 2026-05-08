package com.campusflow.domain.career.dto;

import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CareerActivityResponse(
        Long id,
        ActivityType type,
        String typeLabel,
        ActivityStatus status,
        String statusLabel,
        String title,
        String organization,
        LocalDate targetDate,
        LocalDate completedDate,
        String score,
        String memo,
        LocalDateTime createdAt
) {
    public static CareerActivityResponse from(CareerActivity a) {
        return new CareerActivityResponse(
                a.getId(), a.getType(), a.getType().getLabel(),
                a.getStatus(), a.getStatus().getLabel(),
                a.getTitle(), a.getOrganization(),
                a.getTargetDate(), a.getCompletedDate(),
                a.getScore(), a.getMemo(), a.getCreatedAt()
        );
    }
}
