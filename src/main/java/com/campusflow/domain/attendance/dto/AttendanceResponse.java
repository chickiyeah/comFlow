package com.campusflow.domain.attendance.dto;

import com.campusflow.domain.attendance.entity.Attendance;
import com.campusflow.domain.attendance.entity.AttendanceStatus;

import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        String subjectName,
        LocalDate attendanceDate,
        AttendanceStatus status,
        String note
) {
    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(a.getId(), a.getSubjectName(), a.getAttendanceDate(), a.getStatus(), a.getNote());
    }
}
