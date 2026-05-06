package com.campusflow.domain.roadmap.dto;

import java.util.List;

public record RoadmapResponse(
        String jobTitle,
        List<Certificate> certificates,
        List<SemesterPlan> semesterPlans,
        String aiSource // "INTERNAL" or "EXTERNAL"
) {
    public record Certificate(String name, String type, String description) {
        // type: REQUIRED, RECOMMENDED, OPTIONAL
    }

    public record SemesterPlan(int semester, String focus, List<String> tasks) {}
}
