package com.campusflow.domain.review.repository;

import com.campusflow.domain.review.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    List<CourseReview> findBySubjectNameContainingIgnoreCaseOrderByCreatedAtDesc(String subjectName);
    List<CourseReview> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.subjectName = :subjectName")
    Double getAverageRating(String subjectName);
}
