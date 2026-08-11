package com.campusflow.domain.jobpilot.dto;

/**
 * 문항 1개에 대한 자소서 생성 결과 + 글자수 리포트.
 */
public record EssayResult(
        String question,
        String text,
        Integer limit,          // 글자수 제한 (없으면 null)
        int withSpaces,         // 공백 포함 글자수
        int withoutSpaces,      // 공백 제외 글자수
        String status,          // 'ok' | 'over' | 'short' | 'no_limit'
        int attempts,           // 글자수 가드 재생성 횟수
        String flavor           // 적용된 직무 색 key
) {}
