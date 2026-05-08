package com.campusflow.domain.coverletter.repository;

import com.campusflow.domain.coverletter.entity.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
    List<CoverLetter> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
    Optional<CoverLetter> findByIdAndStudentId(Long id, Long studentId);
}
