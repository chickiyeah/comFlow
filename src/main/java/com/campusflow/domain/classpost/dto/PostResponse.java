package com.campusflow.domain.classpost.dto;

import com.campusflow.domain.classpost.entity.ClassPost;
import com.campusflow.domain.material.entity.Material;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String type,
        String body,
        Long authorId,
        String authorName,
        Long materialId,
        String materialTitle,
        LocalDateTime createdAt,
        List<PostCommentResponse> comments
) {
    public static PostResponse from(ClassPost p, List<PostCommentResponse> comments) {
        Material m = p.getMaterial();
        return new PostResponse(
                p.getId(), p.getType().name(), p.getBody(),
                p.getAuthor().getId(), p.getAuthor().getName(),
                m != null ? m.getId() : null,
                m != null ? m.getTitle() : null,
                p.getCreatedAt(), comments
        );
    }
}
