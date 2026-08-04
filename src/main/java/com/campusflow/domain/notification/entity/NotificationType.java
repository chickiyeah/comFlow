package com.campusflow.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    JOB_ALERT("채용 알림", "work"),
    EARLY_WARNING("학사 경보", "warning"),
    NOTICE("공지", "campaign"),
    SYSTEM("시스템", "notifications");

    private final String label;
    private final String icon;   // Material Symbols 아이콘명
}
