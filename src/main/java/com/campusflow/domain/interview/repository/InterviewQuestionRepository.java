package com.campusflow.domain.interview.repository;

import com.campusflow.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySessionIdOrderByQuestionIndex(Long sessionId);

    Optional<InterviewQuestion> findBySessionIdAndQuestionIndex(Long sessionId, int questionIndex);
}
