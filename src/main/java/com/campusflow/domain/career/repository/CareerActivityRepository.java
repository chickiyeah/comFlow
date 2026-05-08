package com.campusflow.domain.career.repository;

import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerActivityRepository extends JpaRepository<CareerActivity, Long> {
    List<CareerActivity> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<CareerActivity> findByStudentIdAndTypeOrderByCreatedAtDesc(Long studentId, ActivityType type);
    Optional<CareerActivity> findByIdAndStudentId(Long id, Long studentId);
    long countByStudentIdAndType(Long studentId, ActivityType type);
}
