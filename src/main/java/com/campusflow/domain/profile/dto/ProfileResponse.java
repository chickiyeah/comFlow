package com.campusflow.domain.profile.dto;

import com.campusflow.domain.student.entity.Student;

import java.time.LocalDateTime;

public record ProfileResponse(
        String studentId,
        String name,
        String department,
        int grade,
        int semester,
        String phone,
        String email,
        String profileImageData,
        boolean intranetSyncEnabled,
        LocalDateTime intranetSyncedAt,
        boolean hasPortalToken,
        boolean hasPortalPassword
) {
    public static ProfileResponse from(Student s) {
        return new ProfileResponse(
                s.getStudentId(),
                s.getName(),
                s.getDepartment(),
                s.getGrade(),
                s.getSemester(),
                s.getPhone(),
                s.getEmail(),
                s.getProfileImageData(),
                Boolean.TRUE.equals(s.getIntranetSyncEnabled()),
                s.getIntranetSyncedAt(),
                s.getPortalAccessToken() != null,
                s.getPortalPassword() != null
        );
    }
}
