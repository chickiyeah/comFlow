package com.campusflow.domain.interview.repository;

import com.campusflow.domain.interview.entity.InterviewSession;
import com.campusflow.domain.interview.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<InterviewSession> findByStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, InterviewStatus status);
}
