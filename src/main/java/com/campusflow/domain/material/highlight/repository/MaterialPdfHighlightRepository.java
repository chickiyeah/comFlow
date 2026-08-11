package com.campusflow.domain.material.highlight.repository;

import com.campusflow.domain.material.highlight.entity.MaterialPdfHighlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialPdfHighlightRepository extends JpaRepository<MaterialPdfHighlight, Long> {
    List<MaterialPdfHighlight> findByAnalysisIdOrderByPageNumberAscDisplayOrderAsc(Long analysisId);
    void deleteByAnalysisId(Long analysisId);
}
