package com.campusflow.domain.assignment.repository;

import com.campusflow.domain.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByClassRoomIdOrderByCreatedAtDesc(Long classRoomId);
    List<Assignment> findByClassRoomIdAndDraftFalseOrderByCreatedAtDesc(Long classRoomId);
}
