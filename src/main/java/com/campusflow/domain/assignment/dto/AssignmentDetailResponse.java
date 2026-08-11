package com.campusflow.domain.assignment.dto;

import com.campusflow.domain.assignment.entity.Assignment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 과제 상세. {@code mySubmission}은 학생 본인 제출(없으면 null),
 * {@code stats}는 교사에게만 채워짐(학생은 null).
 */
public record AssignmentDetailResponse(
        Long id,
        Long classId,
        String title,
        String instructions,
        LocalDateTime dueDate,
        int points,
        boolean draft,
        String topic,
        String createdByName,
        LocalDateTime createdAt,
        boolean amTeacher,
        List<AssignmentFileResponse> files,
        SubmissionResponse mySubmission,
        SubmissionStatsResponse stats
) {
    public static AssignmentDetailResponse of(Assignment a, boolean amTeacher,
                                              List<AssignmentFileResponse> files,
                                              SubmissionResponse mySubmission,
                                              SubmissionStatsResponse stats) {
        return new AssignmentDetailResponse(
                a.getId(), a.getClassRoom().getId(), a.getTitle(), a.getInstructions(),
                a.getDueDate(), a.getPoints(), a.isDraft(), a.getTopic(),
                a.getCreatedBy() != null ? a.getCreatedBy().getName() : null,
                a.getCreatedAt(), amTeacher, files, mySubmission, stats
        );
    }
}
