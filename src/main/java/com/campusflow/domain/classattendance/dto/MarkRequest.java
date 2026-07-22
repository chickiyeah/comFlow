package com.campusflow.domain.classattendance.dto;

import com.campusflow.domain.classattendance.entity.ClassAttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record MarkRequest(
        @NotNull(message = "학생 ID는 필수입니다.")
        Long studentId,

        @NotNull(message = "출결 상태는 필수입니다.")
        ClassAttendanceStatus status
) {
}
