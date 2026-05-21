package com.campusflow.domain.planner.dto;

import java.util.List;

public record StudyPlanRequest(
        List<ExamInfo> exams,
        List<String> weakSubjects
) {
    public record ExamInfo(String subject, String examDate) {}
}
