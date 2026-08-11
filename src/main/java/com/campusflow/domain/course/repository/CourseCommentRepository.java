package com.campusflow.domain.course.repository;

import com.campusflow.domain.course.entity.CourseComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseCommentRepository extends JpaRepository<CourseComment, Long> {
    List<CourseComment> findByCourseIdOrderByCreatedAtAsc(Long courseId);
}
