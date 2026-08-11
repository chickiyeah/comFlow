package com.campusflow.domain.material.dto;

import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.storage.entity.StoredFile;

import java.time.LocalDateTime;

/** 자료 목록 아이템 (파일 바이트/스트림URL 제외). */
public record MaterialResponse(
        Long id,
        String title,
        Integer week,
        String topic,
        boolean hasFile,
        Long fileId,
        String filename,
        String contentType,
        String createdByName,
        boolean hasSummary,
        LocalDateTime createdAt
) {
    public static MaterialResponse from(Material m, boolean hasSummary) {
        StoredFile f = m.getStoredFile();
        return new MaterialResponse(
                m.getId(), m.getTitle(), m.getWeek(), m.getTopic(),
                f != null, f != null ? f.getId() : null,
                f != null ? f.getOriginalFilename() : null,
                f != null ? f.getContentType() : null,
                m.getCreatedBy() != null ? m.getCreatedBy().getName() : null,
                hasSummary,
                m.getCreatedAt()
        );
    }
}
