package com.campusflow.domain.deptinfo.dto;

import com.campusflow.domain.deptinfo.entity.DeptInfo;

import java.time.LocalDateTime;

public record DeptInfoResponse(
        Long id,
        String category,
        String categoryLabel,
        String title,
        String content,
        String keywords,
        boolean active,
        LocalDateTime updatedAt
) {
    public static DeptInfoResponse from(DeptInfo d) {
        return new DeptInfoResponse(
                d.getId(),
                d.getCategory().name(),
                d.getCategory().getLabel(),
                d.getTitle(),
                d.getContent(),
                d.getKeywords(),
                d.isActive(),
                d.getUpdatedAt()
        );
    }
}
