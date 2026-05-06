package com.campusflow.domain.graduation.repository;

import com.campusflow.domain.graduation.entity.GraduationRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GraduationRequirementRepository extends JpaRepository<GraduationRequirement, Long> {
    List<GraduationRequirement> findByDepartment(String department);
}
