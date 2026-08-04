package com.campusflow.domain.course.dto;

import com.campusflow.domain.course.entity.CourseComment;

import java.time.LocalDateTime;

public record CourseCommentResponse(
        Long id,
        Long parentId,
        String authorName,
        String content,
        boolean question,
        boolean staff,
        boolean aiGenerated,
        LocalDateTime createdAt
) {
    public static CourseCommentResponse from(CourseComment c) {
        return new CourseCommentResponse(
                c.getId(), c.getParentId(), c.getAuthorName(), c.getContent(),
                c.isQuestion(), c.isStaff(), c.isAiGenerated(), c.getCreatedAt());
    }
}
