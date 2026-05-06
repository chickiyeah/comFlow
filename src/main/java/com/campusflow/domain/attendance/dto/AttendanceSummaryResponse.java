package com.campusflow.domain.attendance.dto;

import java.util.List;

public record AttendanceSummaryResponse(
        long totalPresent,
        long totalLate,
        long totalAbsent,
        long totalExcused,
        List<String> absenceWarnings, // 결석 3회 이상 과목
        List<AttendanceResponse> records
) {}
