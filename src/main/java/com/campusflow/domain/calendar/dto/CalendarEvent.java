package com.campusflow.domain.calendar.dto;

import java.time.LocalDate;

public record CalendarEvent(
        LocalDate date,
        String title,
        String type,   // LECTURE | NOTICE | CERT_EXAM
        String description,
        String color
) {}
