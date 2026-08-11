package com.campusflow.domain.classroom.repository;

import com.campusflow.domain.classroom.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByCode(String code);
    boolean existsByCode(String code);
}
