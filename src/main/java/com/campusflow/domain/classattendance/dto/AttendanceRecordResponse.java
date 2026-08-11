package com.campusflow.domain.classattendance.dto;

import com.campusflow.domain.classattendance.entity.ClassAttendanceRecord;

import java.time.LocalDateTime;

public record AttendanceRecordResponse(
        Long studentId,
        String studentName,
        String status,
        LocalDateTime markedAt
) {
    public static AttendanceRecordResponse from(ClassAttendanceRecord r) {
        return new AttendanceRecordResponse(
                r.getStudent().getId(), r.getStudent().getName(), r.getStatus().name(), r.getMarkedAt());
    }
}
