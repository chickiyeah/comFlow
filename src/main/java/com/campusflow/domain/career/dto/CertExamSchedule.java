package com.campusflow.domain.career.dto;

public record CertExamSchedule(
        String examName,
        String qualification,
        String writtenDate,
        String writtenResultDate,
        String practicalDate,
        String practicalResultDate,
        String organization
) {}
