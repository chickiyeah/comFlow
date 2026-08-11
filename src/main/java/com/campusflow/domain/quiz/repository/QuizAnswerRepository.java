package com.campusflow.domain.quiz.repository;

import com.campusflow.domain.quiz.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findBySubmissionId(Long submissionId);
}
