package com.campusflow.domain.material.repository;

import com.campusflow.domain.material.entity.MaterialSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialSummaryRepository extends JpaRepository<MaterialSummary, Long> {
    Optional<MaterialSummary> findByMaterialId(Long materialId);
    boolean existsByMaterialId(Long materialId);
    void deleteByMaterialId(Long materialId);
}
