package com.campusflow.domain.quiz.repository;

import com.campusflow.domain.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByActiveTrueOrderByCreatedAtDesc();
    List<Quiz> findByCourseIdAndActiveTrueOrderByCreatedAtDesc(Long courseId);
    List<Quiz> findAllByOrderByCreatedAtDesc();
}
