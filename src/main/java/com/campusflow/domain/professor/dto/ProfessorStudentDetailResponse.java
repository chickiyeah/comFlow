package com.campusflow.domain.professor.dto;

import java.util.List;

public record ProfessorStudentDetailResponse(
        String studentId,
        String name,
        int grade,
        int semester,
        String department,
        String phone,
        String email,
        double gpa,
        int totalCredits,
        List<GradeItem> grades,
        AttendanceSummary attendance
) {
    public record GradeItem(
            String subjectName,
            String subjectCode,
            int credits,
            String letterGrade,
            double gradePoint,
            int gradeYear,
            int gradeSemester
    ) {}

    public record AttendanceSummary(
            long present,
            long late,
            long absent,
            long excused,
            Integer rate,
            List<String> warnings
    ) {}
}
