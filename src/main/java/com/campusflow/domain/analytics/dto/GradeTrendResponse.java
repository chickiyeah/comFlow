package com.campusflow.domain.analytics.dto;

import java.util.List;

public record GradeTrendResponse(
        List<SemesterStat> semesters,
        double overallGpa,
        int totalCredits
) {
    public record SemesterStat(int year, int semester, double gpa, int credits, String label) {}
}
