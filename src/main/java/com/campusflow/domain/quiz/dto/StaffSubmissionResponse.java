package com.campusflow.domain.quiz.dto;

import com.campusflow.domain.quiz.entity.QuizSubmission;

import java.time.LocalDateTime;

public record StaffSubmissionResponse(
        Long id,
        String studentName,
        int score,
        int maxScore,
        boolean needsReview,
        LocalDateTime submittedAt
) {
    public static StaffSubmissionResponse from(QuizSubmission s) {
        return new StaffSubmissionResponse(s.getId(), s.getStudentName(), s.getScore(),
                s.getMaxScore(), s.isNeedsReview(), s.getSubmittedAt());
    }
}
