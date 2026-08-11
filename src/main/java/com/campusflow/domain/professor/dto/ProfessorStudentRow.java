package com.campusflow.domain.professor.dto;

public record ProfessorStudentRow(
        Long id,
        String studentId,
        String name,
        int grade,
        int semester,
        String department,
        double gpa,
        Integer attendanceRate,
        boolean atRisk
) {}
