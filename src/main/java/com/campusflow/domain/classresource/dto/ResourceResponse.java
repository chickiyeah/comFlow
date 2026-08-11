package com.campusflow.domain.classresource.dto;

import com.campusflow.domain.classresource.entity.ClassResource;
import com.campusflow.domain.storage.entity.StoredFile;

import java.time.LocalDateTime;

/** LINK면 url, FILE이면 streamUrl(요청자 티켓 포함)/filename이 채워진다. */
public record ResourceResponse(
        Long id,
        String type,
        String title,
        String url,
        Long fileId,
        String filename,
        String streamUrl,
        String createdByName,
        LocalDateTime createdAt
) {
    public static ResourceResponse from(ClassResource r, String streamUrl) {
        StoredFile f = r.getStoredFile();
        return new ResourceResponse(
                r.getId(), r.getType().name(), r.getTitle(), r.getUrl(),
                f != null ? f.getId() : null,
                f != null ? f.getOriginalFilename() : null,
                streamUrl,
                r.getCreatedBy() != null ? r.getCreatedBy().getName() : null,
                r.getCreatedAt()
        );
    }
}
