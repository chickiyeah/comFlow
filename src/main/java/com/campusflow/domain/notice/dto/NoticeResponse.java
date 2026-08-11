package com.campusflow.domain.notice.dto;

import com.campusflow.domain.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String summary,
        String content,
        boolean important,
        LocalDateTime createdAt
) {
    public static NoticeResponse from(Notice n) {
        return new NoticeResponse(n.getId(), n.getTitle(), n.getSummary(), n.getContent(), n.isImportant(), n.getCreatedAt());
    }
}
