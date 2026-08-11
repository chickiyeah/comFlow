package com.campusflow.domain.classresource.repository;

import com.campusflow.domain.classresource.entity.ClassResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassResourceRepository extends JpaRepository<ClassResource, Long> {
    List<ClassResource> findByClassRoomIdOrderByCreatedAtDesc(Long classRoomId);
}
