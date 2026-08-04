package com.campusflow.domain.deptinfo.repository;

import com.campusflow.domain.deptinfo.entity.DeptInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptInfoRepository extends JpaRepository<DeptInfo, Long> {

    List<DeptInfo> findByActiveTrueOrderByCategoryAsc();

    List<DeptInfo> findAllByOrderByCreatedAtDesc();
}
