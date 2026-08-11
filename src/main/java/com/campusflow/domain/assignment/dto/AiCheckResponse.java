package com.campusflow.domain.assignment.dto;

import java.util.List;

/** AI 제출물 완성도 점검 결과 (저장 안 함 — 교사 검토용 제안). */
public record AiCheckResponse(
        Integer suggestedScore,
        String feedback,
        List<String> strengths,
        List<String> improvements
) {
}
