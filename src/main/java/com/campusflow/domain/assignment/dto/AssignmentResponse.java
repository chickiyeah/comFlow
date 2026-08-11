package com.campusflow.domain.assignment.dto;

import com.campusflow.domain.assignment.entity.Assignment;

import java.time.LocalDateTime;

/** 과제 목록 아이템. {@code mySubmissionStatus}는 학생 본인 제출 상태(없으면 null). */
public record AssignmentResponse(
        Long id,
        String title,
        LocalDateTime dueDate,
        int points,
        boolean draft,
        String topic,
        String createdByName,
        String mySubmissionStatus,
        LocalDateTime createdAt
) {
    public static AssignmentResponse from(Assignment a, String mySubmissionStatus) {
        return new AssignmentResponse(
                a.getId(), a.getTitle(), a.getDueDate(), a.getPoints(), a.isDraft(), a.getTopic(),
                a.getCreatedBy() != null ? a.getCreatedBy().getName() : null,
                mySubmissionStatus,
                a.getCreatedAt()
        );
    }
}
