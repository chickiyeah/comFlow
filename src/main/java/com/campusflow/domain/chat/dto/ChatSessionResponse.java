package com.campusflow.domain.chat.dto;

import com.campusflow.domain.chat.entity.ChatSession;

import java.time.LocalDateTime;

public record ChatSessionResponse(
        Long id,
        String title,
        LocalDateTime updatedAt
) {
    public static ChatSessionResponse from(ChatSession s) {
        return new ChatSessionResponse(s.getId(), s.getTitle(), s.getUpdatedAt());
    }
}
