package com.campusflow.domain.notification.dto;

import com.campusflow.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String icon,
        String title,
        String body,
        String link,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getType().getIcon(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.isReadFlag(),
                n.getCreatedAt()
        );
    }
}
