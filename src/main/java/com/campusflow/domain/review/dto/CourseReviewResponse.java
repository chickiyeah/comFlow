package com.campusflow.domain.review.dto;

import com.campusflow.domain.review.entity.CourseReview;

import java.time.LocalDateTime;

public record CourseReviewResponse(
        Long id,
        String subjectName,
        String professor,
        int year,
        int semester,
        int rating,
        String content,
        String authorName,
        boolean anonymous,
        boolean isMine,
        LocalDateTime createdAt
) {
    public static CourseReviewResponse from(CourseReview r, Long myStudentId) {
        boolean isMine = r.getStudent().getId().equals(myStudentId);
        String author  = r.isAnonymous() ? "익명" : r.getStudent().getName();
        return new CourseReviewResponse(
                r.getId(), r.getSubjectName(), r.getProfessor(),
                r.getYear(), r.getSemester(), r.getRating(), r.getContent(),
                author, r.isAnonymous(), isMine, r.getCreatedAt()
        );
    }
}
