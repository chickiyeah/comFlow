package com.campusflow.domain.quiz.repository;

import com.campusflow.domain.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizIdOrderBySeqAsc(Long quizId);
    void deleteByQuizId(Long quizId);
}
