package com.campusflow.domain.grade.dto;

import com.campusflow.domain.grade.entity.Grade;

public record GradeResponse(
        Long id,
        String subjectName,
        String subjectCode,
        int credits,
        String letterGrade,
        double gradePoint,
        int gradeYear,
        int gradeSemester
) {
    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
                grade.getId(),
                grade.getSubjectName(),
                grade.getSubjectCode(),
                grade.getCredits(),
                grade.getLetterGrade(),
                grade.getGradePoint(),
                grade.getGradeYear(),
                grade.getGradeSemester()
        );
    }
}
