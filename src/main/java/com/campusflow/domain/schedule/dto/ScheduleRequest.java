package com.campusflow.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;

public record ScheduleRequest(
        @NotBlank String subjectName,
        String subjectCode,
        String professor,
        String room,
        @NotNull DayOfWeek dayOfWeek,
        @NotBlank String startTime,
        @NotBlank String endTime,
        int year,
        int semester
) {}
