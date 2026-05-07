package com.campusflow.domain.suggestion.dto;

import com.campusflow.domain.suggestion.entity.Suggestion;
import com.campusflow.domain.suggestion.entity.SuggestionCategory;
import com.campusflow.domain.suggestion.entity.SuggestionStatus;

import java.time.LocalDateTime;

public record SuggestionResponse(
        Long id,
        SuggestionCategory category,
        String content,
        String adminReply,
        SuggestionStatus status,
        LocalDateTime createdAt
) {
    public static SuggestionResponse from(Suggestion s) {
        return new SuggestionResponse(
                s.getId(), s.getCategory(), s.getContent(),
                s.getAdminReply(), s.getStatus(), s.getCreatedAt()
        );
    }
}
