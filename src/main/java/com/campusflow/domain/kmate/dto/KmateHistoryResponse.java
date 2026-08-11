package com.campusflow.domain.kmate.dto;

import com.campusflow.domain.kmate.entity.KmateHistory;

import java.time.LocalDateTime;

public record KmateHistoryResponse(
        Long id,
        String question,
        String answer,
        LocalDateTime createdAt
) {
    public static KmateHistoryResponse from(KmateHistory h) {
        return new KmateHistoryResponse(h.getId(), h.getQuestion(), h.getAnswer(), h.getCreatedAt());
    }
}
