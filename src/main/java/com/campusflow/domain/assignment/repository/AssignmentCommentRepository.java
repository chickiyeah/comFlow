package com.campusflow.domain.assignment.repository;

import com.campusflow.domain.assignment.entity.AssignmentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentCommentRepository extends JpaRepository<AssignmentComment, Long> {
    List<AssignmentComment> findByAssignmentIdAndStudentIdOrderByCreatedAtAsc(Long assignmentId, Long studentId);
    void deleteByAssignmentId(Long assignmentId);
}
