package com.campusflow.domain.quiz.dto;

import com.campusflow.domain.quiz.entity.Quiz;

import java.time.LocalDateTime;

public record QuizResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        String instructorName,
        int questionCount,
        int totalPoints,
        boolean active,
        LocalDateTime createdAt
) {
    public static QuizResponse from(Quiz q, int questionCount, int totalPoints) {
        return new QuizResponse(q.getId(), q.getCourseId(), q.getTitle(), q.getDescription(),
                q.getInstructorName(), questionCount, totalPoints, q.isActive(), q.getCreatedAt());
    }
}
