package com.campusflow.domain.coverletter.dto;

import com.campusflow.domain.coverletter.entity.CoverLetter;

import java.time.LocalDateTime;

public record CoverLetterResponse(
        Long id,
        String title,
        String companyName,
        String jobTitle,
        String content,
        String preview,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CoverLetterResponse from(CoverLetter c) {
        String preview = c.getContent().length() > 120
                ? c.getContent().substring(0, 120) + "..."
                : c.getContent();
        return new CoverLetterResponse(
                c.getId(), c.getTitle(), c.getCompanyName(), c.getJobTitle(),
                c.getContent(), preview, c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
