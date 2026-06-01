package com.campusflow.domain.chat.dto;

import java.util.List;

public record ChatSessionDetailResponse(
        Long id,
        String title,
        List<ChatMessageResponse> messages
) {}
