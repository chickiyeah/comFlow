package com.campusflow.domain.assignment.dto;

import com.campusflow.domain.assignment.entity.AssignmentComment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long authorId,
        String authorName,
        String body,
        LocalDateTime createdAt
) {
    public static CommentResponse from(AssignmentComment c) {
        return new CommentResponse(c.getId(), c.getAuthor().getId(), c.getAuthor().getName(),
                c.getBody(), c.getCreatedAt());
    }
}
