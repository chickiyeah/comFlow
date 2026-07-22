package com.campusflow.domain.assignment.dto;

import com.campusflow.domain.assignment.entity.Submission;
import com.campusflow.domain.storage.entity.StoredFile;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String content,
        Long fileId,
        String filename,
        String streamUrl,
        String status,
        Integer grade,
        String feedback,
        LocalDateTime submittedAt,
        LocalDateTime gradedAt
) {
    public static SubmissionResponse from(Submission s, String streamUrl) {
        StoredFile f = s.getStoredFile();
        return new SubmissionResponse(
                s.getId(),
                s.getAssignment().getId(),
                s.getStudent().getId(),
                s.getStudent().getName(),
                s.getContent(),
                f != null ? f.getId() : null,
                f != null ? f.getOriginalFilename() : null,
                streamUrl,
                s.getStatus().name(),
                s.getGrade(),
                s.getFeedback(),
                s.getSubmittedAt(),
                s.getGradedAt()
        );
    }
}
