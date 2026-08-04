package com.campusflow.domain.course.dto;

public record CourseProgressResponse(
        Long courseId,
        int lastPositionSec,
        int durationSec,
        boolean completed
) {}
