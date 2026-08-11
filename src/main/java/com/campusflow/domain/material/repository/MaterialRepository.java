package com.campusflow.domain.material.repository;

import com.campusflow.domain.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByClassRoomIdOrderByWeekAscCreatedAtAsc(Long classRoomId);
    long countByClassRoomId(Long classRoomId);
}
