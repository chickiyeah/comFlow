package com.campusflow.domain.award.repository;

import com.campusflow.domain.award.entity.Award;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AwardRepository extends JpaRepository<Award, Long> {
    List<Award> findByStudentIdOrderByAwardDateDesc(Long studentId);
}
