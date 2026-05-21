package com.campusflow.domain.study.repository;

import com.campusflow.domain.study.entity.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyMemberRepository extends JpaRepository<StudyMember, Long> {
    List<StudyMember> findByGroupId(Long groupId);
    List<StudyMember> findByStudentId(Long studentId);
    Optional<StudyMember> findByGroupIdAndStudentId(Long groupId, Long studentId);
    int countByGroupId(Long groupId);
    boolean existsByGroupIdAndStudentId(Long groupId, Long studentId);
}
