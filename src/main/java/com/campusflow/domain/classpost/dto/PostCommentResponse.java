package com.campusflow.domain.classpost.dto;

import com.campusflow.domain.classpost.entity.PostComment;

import java.time.LocalDateTime;

public record PostCommentResponse(
        Long id,
        Long authorId,
        String authorName,
        String body,
        LocalDateTime createdAt
) {
    public static PostCommentResponse from(PostComment c) {
        return new PostCommentResponse(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(),
                c.getBody(), c.getCreatedAt());
    }
}
