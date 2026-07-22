package com.campusflow.domain.material.dto;

import com.campusflow.domain.material.entity.MaterialSummary;

public record MaterialSummaryResponse(
        String shortSummary,
        String paragraphSummary,
        String detailedSummary
) {
    public static MaterialSummaryResponse from(MaterialSummary s) {
        return new MaterialSummaryResponse(s.getShortSummary(), s.getParagraphSummary(), s.getDetailedSummary());
    }
}
