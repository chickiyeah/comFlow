package com.campusflow.domain.material.highlight.dto;

import com.campusflow.domain.material.highlight.entity.MaterialHighlightAnalysis;
import com.campusflow.domain.material.highlight.entity.MaterialPdfHighlight;

import java.util.List;

/** 분석 상태 + (READY일 때) 하이라이트 목록. */
public record HighlightResponse(
        Long analysisId,
        String status,
        int totalPages,
        int completedPages,
        String errorMessage,
        List<HighlightItem> highlights
) {
    public record HighlightItem(int pageNumber, String excerpt, String explanation, String category) {
        public static HighlightItem from(MaterialPdfHighlight h) {
            return new HighlightItem(h.getPageNumber(), h.getExcerpt(), h.getExplanation(), h.getCategory());
        }
    }

    public static HighlightResponse of(MaterialHighlightAnalysis a, List<MaterialPdfHighlight> highlights) {
        return new HighlightResponse(
                a.getId(), a.getStatus().name(), a.getTotalPages(), a.getCompletedPages(), a.getErrorMessage(),
                highlights.stream().map(HighlightItem::from).toList()
        );
    }
}
