package com.campusflow.domain.material.highlight.repository;

import com.campusflow.domain.material.highlight.entity.MaterialHighlightAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialHighlightAnalysisRepository extends JpaRepository<MaterialHighlightAnalysis, Long> {
    Optional<MaterialHighlightAnalysis> findFirstByMaterialIdOrderByIdDesc(Long materialId);
}
