package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.*;
import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.service.*;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/career/search")
@RequiredArgsConstructor
public class JobSearchController {

    private final WorknetService worknetService;
    private final JobkoreaService jobkoreaService;
    private final Work24ScraperService work24ScraperService;
    private final SaraminService saraminService;
    private final QNetService qNetService;
    private final BlindRecruitService blindRecruitService;
    private final JobImportService jobImportService;
    private final JobKeywordSuggestionService jobKeywordSuggestionService;
    private final DiscordNotifierService discordNotifierService;

    // source: all | jobkorea | work24 | worknet | saramin | imported
    // hideExpired: true(기본) 면 마감 지난 공고 제외
    @GetMapping("/jobs")
    public ApiResponse<List<JobSearchResult>> searchJobs(
            @RequestParam(defaultValue = "IT") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String career,
            @RequestParam(defaultValue = "") String empType,
            @RequestParam(defaultValue = "true") boolean hideExpired) {
        List<JobSearchResult> results = new ArrayList<>();
        switch (source) {
            case "imported" -> results.addAll(jobImportService.list(keyword, hideExpired)
                    .stream().map(JobSearchController::toResult).toList());
            case "worknet"  -> results.addAll(worknetService.searchJobs(keyword, page + 1));
            case "jobkorea" -> results.addAll(jobkoreaService.searchJobs(keyword, page, region, career, empType));
            case "work24"   -> results.addAll(work24ScraperService.searchJobs(keyword, region, career, empType, page + 1));
            case "saramin"  -> results.addAll(saraminService.searchJobs(keyword, page, region, career, empType));
            default -> {
                results.addAll(jobkoreaService.searchJobs(keyword, page, region, career, empType));
                results.addAll(work24ScraperService.searchJobs(keyword, region, career, empType, page + 1));
                results.addAll(saraminService.searchJobs(keyword, page, region, career, empType));
                results.addAll(worknetService.searchJobs(keyword, page + 1));
            }
        }
        // 'imported'는 이미 필터·정렬됨. 그 외 라이브 결과는 후처리(중복제거·만료필터·마감임박순)
        if (!"imported".equals(source)) {
            results = postProcess(results, hideExpired);
        }
        return ApiResponse.ok(results);
    }

    /** 백그라운드 적재 공고 조회 (공공 출처). hideExpired=true 면 지난 공고 제외. */
    @GetMapping("/imported-jobs")
    public ApiResponse<List<JobSearchResult>> importedJobs(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "true") boolean hideExpired) {
        List<JobSearchResult> results = jobImportService.list(keyword, hideExpired)
                .stream().map(JobSearchController::toResult).toList();
        return ApiResponse.ok(results);
    }

    /** 수동 수집 트리거 (인증 필요 — POST는 permitAll 대상 아님). 쿨다운 시 메시지로 안내. */
    @PostMapping("/imported-jobs/refresh")
    public ApiResponse<Integer> refreshImportedJobs() {
        int n = jobImportService.refreshIfAllowed();
        if (n < 0) return ApiResponse.ok("최근에 수집했습니다. 잠시 후 다시 시도하세요.", 0);
        return ApiResponse.ok(n + "건 수집/갱신 완료", n);
    }

    /** 중복 제거(url 기준) + 만료(마감 지난) 필터 + 마감 임박순 정렬. */
    private static List<JobSearchResult> postProcess(List<JobSearchResult> results, boolean hideExpired) {
        LocalDate today = LocalDate.now();
        Map<String, JobSearchResult> unique = new LinkedHashMap<>();
        for (JobSearchResult r : results) {
            if (r == null) continue;
            if (hideExpired && r.deadline() != null && r.deadline().isBefore(today)) continue;
            String key = (r.url() != null && !r.url().isBlank())
                    ? r.url()
                    : (r.title() + "|" + r.company());
            unique.putIfAbsent(key, r);
        }
        // 마감 임박순(가까운 마감 먼저), 마감 없음(상시)은 뒤로
        return unique.values().stream()
                .sorted(Comparator.comparing(JobSearchResult::deadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static JobSearchResult toResult(ImportedJob j) {
        return new JobSearchResult(
                "imported-" + j.getId(), j.getTitle(), j.getCompany(), j.getLocation(),
                j.getUrl(), j.getDeadline(), j.getJobType(), j.getSalary(),
                j.getSource() != null ? j.getSource() : "work24");
    }

    /** 로그인 학생의 희망직무·보유기술 기반 검색 기본 키워드 + "이 직무는 어때요?" 추천 칩. */
    @GetMapping("/keyword-suggestions")
    public ApiResponse<KeywordSuggestionResponse> keywordSuggestions(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(jobKeywordSuggestionService.suggest(username));
    }

    @GetMapping("/certs/schedules")
    public ApiResponse<List<CertExamSchedule>> getSchedules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(qNetService.getSchedules(keyword, year != null ? year : LocalDate.now().getYear()));
    }

    @GetMapping("/certs/list")
    public ApiResponse<List<QualificationItem>> searchQualifications(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(qNetService.searchQualifications(keyword));
    }

    @GetMapping("/certs/detail")
    public ApiResponse<List<QualificationDetail>> getQualificationDetail(
            @RequestParam(required = false) String jmCd,
            @RequestParam(required = false) String qualgbCd) {
        return ApiResponse.ok(qNetService.getQualificationDetail(jmCd, qualgbCd));
    }

    @GetMapping("/certs/locations")
    public ApiResponse<List<ExamLocation>> getExamLocations(
            @RequestParam(required = false) String brchCd) {
        return ApiResponse.ok(qNetService.getExamLocations(brchCd));
    }

    @GetMapping("/blind-recruit")
    public ApiResponse<List<BlindRecruitCompany>> searchBlindRecruit(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return ApiResponse.ok(blindRecruitService.search(keyword, page));
    }

    /** Discord 웹훅 알림 배선 테스트. 전송 건수(웹훅 미설정이면 -1) 반환. */
    @PostMapping("/discord/test")
    public ApiResponse<Integer> discordTest(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(discordNotifierService.notifyTest());
    }
}
