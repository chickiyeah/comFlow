package com.campusflow.domain.course.repository;

import com.campusflow.domain.course.entity.CourseView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseViewRepository extends JpaRepository<CourseView, Long> {
    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);
    Optional<CourseView> findByCourseIdAndStudentId(Long courseId, Long studentId);
    java.util.List<CourseView> findByStudentId(Long studentId);
}
