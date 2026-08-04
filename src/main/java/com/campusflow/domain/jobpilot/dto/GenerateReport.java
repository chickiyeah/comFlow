package com.campusflow.domain.jobpilot.dto;

import java.util.List;

/**
 * [생성] 모듈 산출물. 문항별 자소서 전체 + 메타.
 */
public record GenerateReport(
        String company,
        String position,
        String flavor,          // 판별된 직무 색 key
        String flavorLabel,     // 직무 색 한글 라벨
        List<EssayResult> essays
) {}
