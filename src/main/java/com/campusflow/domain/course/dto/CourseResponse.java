package com.campusflow.domain.course.dto;

import com.campusflow.domain.course.entity.OnlineCourse;

import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        String title,
        String description,
        String videoUrl,
        String thumbnailUrl,
        String category,
        String instructorName,
        int viewCount,
        boolean active,
        LocalDateTime createdAt
) {
    public static CourseResponse from(OnlineCourse c) {
        return new CourseResponse(
                c.getId(), c.getTitle(), c.getDescription(), c.getVideoUrl(),
                c.getThumbnailUrl(), c.getCategory(), c.getInstructorName(),
                c.getViewCount(), c.isActive(), c.getCreatedAt());
    }
}
