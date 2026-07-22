package com.campusflow.domain.classpost.repository;

import com.campusflow.domain.classpost.entity.ClassPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassPostRepository extends JpaRepository<ClassPost, Long> {
    List<ClassPost> findByClassRoomIdOrderByCreatedAtDesc(Long classRoomId);
}
