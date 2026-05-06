package com.campusflow.domain.grade.dto;

import java.util.List;

public record GradeSummaryResponse(
        double gpa,
        int totalCredits,
        List<GradeResponse> grades
) {}
