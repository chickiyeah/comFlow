package com.campusflow.domain.material.dto;

import com.campusflow.domain.material.entity.Material;
import com.campusflow.domain.storage.entity.StoredFile;

import java.time.LocalDateTime;

/** 자료 상세. {@code streamUrl}은 요청 사용자용 단기 티켓이 포함된 파일 스트리밍 URL(파일 있을 때만). */
public record MaterialDetailResponse(
        Long id,
        Long classId,
        String title,
        String instructions,
        Integer week,
        String topic,
        boolean hasFile,
        Long fileId,
        String filename,
        String contentType,
        String streamUrl,
        String textContent,
        String createdByName,
        LocalDateTime createdAt
) {
    public static MaterialDetailResponse from(Material m, String streamUrl) {
        StoredFile f = m.getStoredFile();
        return new MaterialDetailResponse(
                m.getId(), m.getClassRoom().getId(), m.getTitle(), m.getInstructions(),
                m.getWeek(), m.getTopic(),
                f != null, f != null ? f.getId() : null,
                f != null ? f.getOriginalFilename() : null,
                f != null ? f.getContentType() : null,
                streamUrl,
                m.getTextContent(),
                m.getCreatedBy() != null ? m.getCreatedBy().getName() : null,
                m.getCreatedAt()
        );
    }
}
