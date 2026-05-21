package com.campusflow.domain.jobalert.dto;

import com.campusflow.domain.jobalert.entity.JobAlert;

import java.time.LocalDateTime;

public record JobAlertResponse(
        Long id,
        String keyword,
        String region,
        String empType,
        LocalDateTime lastNotifiedAt,
        LocalDateTime createdAt
) {
    public static JobAlertResponse from(JobAlert a) {
        return new JobAlertResponse(a.getId(), a.getKeyword(), a.getRegion(),
                a.getEmpType(), a.getLastNotifiedAt(), a.getCreatedAt());
    }
}
