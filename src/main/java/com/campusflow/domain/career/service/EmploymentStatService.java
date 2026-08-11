package com.campusflow.domain.career.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.career.dto.JobMarketStats;
import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 개인별 취업 통계 — 희망 직무 기반 예상 연봉·요구 학력/자격 정보.
 * <p>실제 채용공고(잡코리아)를 집계해 연봉 표본·상위 기업·지역/경력 분포를 만들고,
 * AI로 예상 연봉·요구 학력·핵심 스킬·추천 자격증·전망을 보강한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmploymentStatService {

    private final JobkoreaService jobkoreaService;
    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    // @Cacheable 자기호출(self-invocation) 시 프록시를 거치도록 자기 자신 주입
    @Lazy
    @Autowired
    private EmploymentStatService self;

    // payRange 문자열에서 "2,400만원", "3600 만원" 같은 금액 추출 (만원 단위)
    private static final Pattern SALARY_PATTERN = Pattern.compile("([0-9][0-9,]{2,})\\s*만\\s*원");

    private static final String SYSTEM_PROMPT = """
            당신은 한국 IT 취업 시장 분석 전문가입니다.
            2년제 컴퓨터정보과(초대졸) 졸업생 관점에서 특정 직무의 취업 시장 정보를 제공하세요.
            과장 없이 현실적인 한국 시장 기준으로 답변하고, 반드시 JSON 형식으로만 반환하세요.
            """;

    private static final String JSON_TEMPLATE = """
            "%s" 직무에 대해 다음 JSON 형식으로만 응답하세요:
            {
              "expectedSalary": "신입(초대졸) 기준 예상 연봉 범위 (예: 2,800~3,500만원)",
              "requiredEducation": "일반적으로 요구되는 학력 (예: 학력무관, 초대졸 이상, 대졸 이상)",
              "coreSkills": ["핵심 기술/역량 5개"],
              "recommendedCerts": ["취업에 유리한 자격증 3~5개"],
              "outlook": "해당 직무의 채용 전망과 진입 난이도를 2~3문장으로"
            }
            """;

    /**
     * 학생의 희망 직무(또는 override)에 대한 취업 통계.
     * jobTitle 미지정 시 저장된 desiredJob을 사용하며 둘 다 없으면 INVALID_INPUT.
     */
    public JobMarketStats getStatistics(String username, String jobTitleOverride) {
        String jobTitle = (jobTitleOverride != null && !jobTitleOverride.isBlank())
                ? jobTitleOverride.trim()
                : getStudent(username).getDesiredJob();

        if (jobTitle == null || jobTitle.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return self.marketStats(jobTitle);
    }

    /** 직무명 기준 시장 통계 (1시간 캐시 — 직무별 공유). */
    @Cacheable(value = "jobStatistics", key = "#jobTitle")
    public JobMarketStats marketStats(String jobTitle) {
        // 잡코리아 2페이지(최대 40건) 수집
        List<JobSearchResult> postings = new ArrayList<>();
        for (int page = 0; page < 2; page++) {
            try {
                postings.addAll(jobkoreaService.searchJobs(jobTitle, page, "", "", ""));
            } catch (Exception e) {
                log.warn("[취업통계] 공고 수집 실패 (page={}): {}", page, e.getMessage());
            }
        }

        JobMarketStats.SalaryInfo salary = aggregateSalary(postings);
        List<JobMarketStats.NameCount> topCompanies = topCounts(
                postings.stream().map(JobSearchResult::company), 6);
        List<JobMarketStats.NameCount> regionDist = topCounts(
                postings.stream().map(JobSearchResult::location).map(EmploymentStatService::sido), 6);
        List<JobMarketStats.NameCount> careerDist = topCounts(
                postings.stream().map(JobSearchResult::jobType), 4);

        JobMarketStats.AiInsight insight = generateInsight(jobTitle);

        return new JobMarketStats(
                jobTitle, postings.size(), salary, insight,
                topCompanies, regionDist, careerDist);
    }

    // ── 연봉 표본 집계 ────────────────────────────────────────
    private JobMarketStats.SalaryInfo aggregateSalary(List<JobSearchResult> postings) {
        List<Integer> values = new ArrayList<>();
        for (JobSearchResult p : postings) {
            if (p.salary() == null) continue;
            Matcher m = SALARY_PATTERN.matcher(p.salary());
            while (m.find()) {
                try {
                    int v = Integer.parseInt(m.group(1).replace(",", ""));
                    // 만원 단위 합리 범위(1,000만~20,000만원)만 채택 — 이상치 제거
                    if (v >= 1000 && v <= 20000) values.add(v);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (values.isEmpty()) return new JobMarketStats.SalaryInfo(null, null, null, 0);
        int min = values.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = values.stream().mapToInt(Integer::intValue).max().orElse(0);
        int avg = (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new JobMarketStats.SalaryInfo(min, max, avg, values.size());
    }

    // ── 상위 N개 빈도 집계 ────────────────────────────────────
    private List<JobMarketStats.NameCount> topCounts(java.util.stream.Stream<String> stream, int limit) {
        return stream
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new JobMarketStats.NameCount(e.getKey(), e.getValue()))
                .toList();
    }

    /** location("서울 강남구")의 시/도만 추출 */
    private static String sido(String location) {
        if (location == null || location.isBlank()) return null;
        String first = location.split("[ ,]")[0].trim();
        return first.isBlank() ? null : first;
    }

    // ── AI 시장 정보 (실패 시 null) ───────────────────────────
    private JobMarketStats.AiInsight generateInsight(String jobTitle) {
        try {
            String raw = aiFacadeService.ask(SYSTEM_PROMPT, String.format(JSON_TEMPLATE, jobTitle));
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            int s = json.indexOf('{'), e = json.lastIndexOf('}');
            if (s < 0 || e <= s) return null;
            json = json.substring(s, e + 1);

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            return new JobMarketStats.AiInsight(
                    asText(parsed.get("expectedSalary")),
                    asText(parsed.get("requiredEducation")),
                    asStringList(parsed.get("coreSkills")),
                    asStringList(parsed.get("recommendedCerts")),
                    asText(parsed.get("outlook"))
            );
        } catch (Exception ex) {
            log.warn("[취업통계] AI 시장정보 생성 실패: {}", ex.getMessage());
            return null;
        }
    }

    private static String asText(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object o) {
        if (!(o instanceof List<?> list)) return List.of();
        return list.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
