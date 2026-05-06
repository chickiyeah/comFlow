package com.campusflow.domain.resume.repository;

import com.campusflow.domain.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
    Optional<Resume> findByIdAndStudentId(Long id, Long studentId);
}
