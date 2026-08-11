package com.campusflow.domain.material.highlight.repository;

import com.campusflow.domain.material.highlight.entity.MaterialHighlightPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialHighlightPageRepository extends JpaRepository<MaterialHighlightPage, Long> {
    List<MaterialHighlightPage> findByAnalysisIdOrderByPageNumberAsc(Long analysisId);
    void deleteByAnalysisId(Long analysisId);
}
