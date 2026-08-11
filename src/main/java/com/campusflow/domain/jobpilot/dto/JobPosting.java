package com.campusflow.domain.jobpilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 채용 공고 1건의 구조화 결과 ([추출] 모듈 산출물).
 * 모든 필드는 '원문에 없으면 null/빈 배열' 원칙 (할루시네이션 방지).
 *
 * 참조구현(jd_extractor/schema.py)을 CampusFlow record 컨벤션으로 포팅.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobPosting(
        // 핵심 식별
        String company,
        String position,
        // 지원 조건
        String employmentType,   // 정규직 | 계약직 | 인턴 | 기타
        String experience,       // 경력 요건 원문 (예: '신입·경력')
        String education,        // 학력 요건 (예: '대졸이상', '무관')
        String location,
        String deadline,         // 마감일 원문 표기 그대로
        String salary,
        // 직무 상세
        List<String> requiredSkills,
        List<String> requirements,
        List<String> preferred,
        List<String> responsibilities,
        // 자소서
        List<EssayQuestion> essayQuestions,
        // 메타
        String rawNotes,
        List<String> missingFields
) {
    /** null 리스트를 빈 리스트로 정규화한 사본을 돌려준다 (NPE 방어). */
    public JobPosting normalized() {
        return new JobPosting(
                company, position, employmentType, experience, education, location,
                deadline, salary,
                orEmpty(requiredSkills), orEmpty(requirements), orEmpty(preferred),
                orEmpty(responsibilities), orEmpty(essayQuestions),
                rawNotes, orEmpty(missingFields)
        );
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
