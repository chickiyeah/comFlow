package com.campusflow.domain.session.dto;

import com.campusflow.domain.session.entity.UserSession;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id,
        String device,
        String ip,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt,
        boolean current
) {
    public static SessionResponse from(UserSession s, String currentJti) {
        return new SessionResponse(
                s.getId(),
                s.getDevice(),
                s.getIp(),
                s.getCreatedAt(),
                s.getLastSeenAt(),
                s.getJti().equals(currentJti)
        );
    }
}
