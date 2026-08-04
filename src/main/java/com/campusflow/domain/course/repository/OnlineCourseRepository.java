package com.campusflow.domain.course.repository;

import com.campusflow.domain.course.entity.OnlineCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnlineCourseRepository extends JpaRepository<OnlineCourse, Long> {
    List<OnlineCourse> findByActiveTrueOrderByCreatedAtDesc();
    List<OnlineCourse> findAllByOrderByCreatedAtDesc();
    List<OnlineCourse> findTop5ByActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
}
