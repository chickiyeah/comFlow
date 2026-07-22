package com.campusflow.domain.classroom.repository;

import com.campusflow.domain.classroom.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {

    Optional<ClassMember> findByClassRoomIdAndUserId(Long classRoomId, Long userId);

    boolean existsByClassRoomIdAndUserId(Long classRoomId, Long userId);

    List<ClassMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    List<ClassMember> findByClassRoomIdOrderByJoinedAtAsc(Long classRoomId);

    long countByClassRoomId(Long classRoomId);
}
