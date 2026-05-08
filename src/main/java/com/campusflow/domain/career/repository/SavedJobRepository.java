package com.campusflow.domain.career.repository;

import com.campusflow.domain.career.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    List<SavedJob> findByStudentIdOrderBySavedAtDesc(Long studentId);
    Optional<SavedJob> findByIdAndStudentId(Long id, Long studentId);
}
