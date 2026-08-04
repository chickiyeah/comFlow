package com.campusflow.domain.quiz.repository;

import com.campusflow.domain.quiz.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    Optional<QuizSubmission> findByQuizIdAndStudentId(Long quizId, Long studentId);
    List<QuizSubmission> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
    List<QuizSubmission> findByStudentId(Long studentId);
}
