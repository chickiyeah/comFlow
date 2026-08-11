package com.campusflow.domain.assignment.dto;

/** 과제 제출 통계 (교사용). */
public record SubmissionStatsResponse(
        long totalStudents,
        long submitted,
        long graded,
        long returned
) {
}
