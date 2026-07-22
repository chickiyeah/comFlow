package com.campusflow.domain.assignment.repository;

import com.campusflow.domain.assignment.entity.AssignmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentFileRepository extends JpaRepository<AssignmentFile, Long> {
    List<AssignmentFile> findByAssignmentId(Long assignmentId);
    void deleteByAssignmentId(Long assignmentId);
}
