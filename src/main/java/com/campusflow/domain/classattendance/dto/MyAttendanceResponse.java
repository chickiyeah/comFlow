package com.campusflow.domain.classattendance.dto;

import com.campusflow.domain.classattendance.entity.ClassAttendanceRecord;

import java.time.LocalDate;

public record MyAttendanceResponse(
        Long sessionId,
        String title,
        LocalDate date,
        String status
) {
    public static MyAttendanceResponse from(ClassAttendanceRecord r) {
        return new MyAttendanceResponse(
                r.getSession().getId(), r.getSession().getTitle(), r.getSession().getDate(), r.getStatus().name());
    }
}
