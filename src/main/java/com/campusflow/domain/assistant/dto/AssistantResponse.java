package com.campusflow.domain.assistant.dto;

import java.util.List;

public record AssistantResponse(
        String studentName,
        int currentSemester,
        int remainingSemesters,
        double gpa,
        String overallAdvice,          // 종합 한줄 평가
        List<String> studyFocus,        // 다음 학기 집중 과목
        List<CertRecommend> certificates,
        List<String> portfolioIdeas,
        List<GradWarning> graduationWarnings,
        List<String> attendanceWarnings
) {
    public record CertRecommend(String name, String reason, String priority) {}
    public record GradWarning(String category, int earned, int required, int shortage) {}
}
