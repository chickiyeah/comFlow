package com.campusflow.domain.professor.dto;

public record ProfessorOverviewResponse(
        long studentCount,
        double avgGpa,
        long atRiskCount
) {}
