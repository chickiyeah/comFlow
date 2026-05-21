package com.campusflow.domain.study.repository;

import com.campusflow.domain.study.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    List<StudyGroup> findBySubjectContainingIgnoreCaseOrderByCreatedAtDesc(String subject);
    List<StudyGroup> findAllByOrderByCreatedAtDesc();
}
