package com.campusflow.domain.classattendance.dto;

import com.campusflow.domain.classattendance.entity.ClassAttendanceSession;

import java.time.LocalDate;
import java.util.List;

/** records는 상세(교사)에서만 채워지고 목록에서는 null. */
public record SessionResponse(
        Long id,
        String title,
        LocalDate date,
        boolean active,
        String openedByName,
        List<AttendanceRecordResponse> records
) {
    public static SessionResponse of(ClassAttendanceSession s, List<AttendanceRecordResponse> records) {
        return new SessionResponse(
                s.getId(), s.getTitle(), s.getDate(), s.isActive(),
                s.getOpenedBy() != null ? s.getOpenedBy().getName() : null,
                records
        );
    }
}
