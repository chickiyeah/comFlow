package com.campusflow.domain.jobpilot.dto;

import java.util.List;

/**
 * [매칭] 모듈 산출물. 공고 요구사항 ↔ 학생 프로필 대조 결과.
 * 이 결과를 [생성]에 주입해 강점은 부각, 약점은 정직하게 처리한다.
 */
public record MatchReport(
        List<String> matchedSkills,
        List<String> missingSkills,
        List<Strength> strengths,   // 근거 경험이 있는 요구사항
        List<Gap> gaps,             // 근거가 없는 요구사항
        String summary              // 한 줄 종합 (생성 프롬프트에 주입)
) {
    /** 매칭된 요구 스킬 + 그 근거가 된 프로필 경험 텍스트. */
    public record Strength(String skill, String evidence) {}

    /** 부족한 요구 스킬 + 심각도. */
    public record Gap(String skill, String severity) {  // severity: 'must' | 'preferred'
    }
}
