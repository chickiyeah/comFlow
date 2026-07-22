package com.campusflow.domain.material.dto;

import com.campusflow.domain.material.entity.MaterialBookmark;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long id,
        int page,
        String note,
        LocalDateTime createdAt
) {
    public static BookmarkResponse from(MaterialBookmark b) {
        return new BookmarkResponse(b.getId(), b.getPage(), b.getNote(), b.getCreatedAt());
    }
}
