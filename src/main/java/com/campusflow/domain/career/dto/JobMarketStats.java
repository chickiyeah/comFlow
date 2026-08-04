package com.campusflow.domain.career.dto;

import java.util.List;

/**
 * 희망 직무 기반 취업 시장 통계.
 * - {@link SalaryInfo}: 실제 채용공고(잡코리아)에서 파싱한 연봉 표본 통계
 * - {@link AiInsight}: AI가 생성한 시장 정보(예상 연봉/요구 학력/핵심 스킬/추천 자격증/전망)
 * - 분포: 실제 공고에서 집계한 상위 채용기업·지역·경력 분포
 */
public record JobMarketStats(
        String jobTitle,
        int totalPostings,
        SalaryInfo salary,
        AiInsight aiInsight,
        List<NameCount> topCompanies,
        List<NameCount> regionDist,
        List<NameCount> careerDist
) {
    /** 실제 공고에서 추출한 연봉(만원) 표본 통계. 표본이 없으면 sampleCount=0. */
    public record SalaryInfo(Integer minManwon, Integer maxManwon, Integer avgManwon, int sampleCount) {}

    /** AI가 생성한 직무 시장 정보. AI 호출 실패 시 null. */
    public record AiInsight(
            String expectedSalary,
            String requiredEducation,
            List<String> coreSkills,
            List<String> recommendedCerts,
            String outlook
    ) {}

    public record NameCount(String name, long count) {}
}
