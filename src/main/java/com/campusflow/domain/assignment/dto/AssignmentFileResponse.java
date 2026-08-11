package com.campusflow.domain.assignment.dto;

import com.campusflow.domain.assignment.entity.AssignmentFile;

public record AssignmentFileResponse(
        Long id,
        Long fileId,
        String filename,
        String contentType,
        String streamUrl
) {
    public static AssignmentFileResponse from(AssignmentFile f, String streamUrl) {
        return new AssignmentFileResponse(
                f.getId(),
                f.getStoredFile().getId(),
                f.getStoredFile().getOriginalFilename(),
                f.getStoredFile().getContentType(),
                streamUrl
        );
    }
}
