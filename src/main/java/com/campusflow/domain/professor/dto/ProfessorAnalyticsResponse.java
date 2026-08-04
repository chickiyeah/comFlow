package com.campusflow.domain.professor.dto;

import java.util.List;

/** 학습분석/조기경보 대시보드 — 활동지표 집계 + 위험도 모델. */
public record ProfessorAnalyticsResponse(
        Aggregate aggregate,
        List<StudentAnalytics> students
) {
    public record Aggregate(
            int studentCount,
            double avgGpa,
            long highRisk,
            long mediumRisk,
            Integer avgAttendance,      // null 가능
            long courseCompletions,
            Integer avgQuizScore        // % null 가능
    ) {}

    public record StudentAnalytics(
            Long id,
            String studentId,
            String name,
            int grade,
            int semester,
            double gpa,
            Integer attendanceRate,
            int courseCompletions,
            Integer quizAvg,            // % null 가능
            int riskScore,              // 0~100
            String riskLevel,           // HIGH | MEDIUM | LOW
            List<String> riskReasons    // 코드: LOW_GPA / ATTENDANCE / NO_ENGAGEMENT / LOW_QUIZ
    ) {}
}
