package com.campusflow.domain.jobalert.repository;

import com.campusflow.domain.jobalert.entity.JobAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobAlertRepository extends JpaRepository<JobAlert, Long> {
    List<JobAlert> findByStudentId(Long studentId);
    List<JobAlert> findAll();
}
