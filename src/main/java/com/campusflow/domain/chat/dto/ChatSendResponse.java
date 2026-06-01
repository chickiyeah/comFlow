package com.campusflow.domain.chat.dto;

public record ChatSendResponse(
        Long sessionId,
        String title,
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage
) {}
