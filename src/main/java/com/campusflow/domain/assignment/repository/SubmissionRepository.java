package com.campusflow.domain.assignment.repository;

import com.campusflow.domain.assignment.entity.Submission;
import com.campusflow.domain.assignment.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    long countByAssignmentId(Long assignmentId);
    long countByAssignmentIdAndStatus(Long assignmentId, SubmissionStatus status);
    void deleteByAssignmentId(Long assignmentId);

    // 성적부(gradebook) — 클래스 스코프 조회
    List<Submission> findByAssignment_ClassRoomId(Long classRoomId);
    List<Submission> findByStudentIdAndAssignment_ClassRoomId(Long studentId, Long classRoomId);
}
