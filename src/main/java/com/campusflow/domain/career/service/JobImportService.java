package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import com.campusflow.domain.career.service.jobfeed.JobFeedCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채용공고 백그라운드 수집·적재 서비스.
 *
 * 소스: 고용24·잡코리아·사람인·워크넷 전체 (사용자 결정으로 민간 포함).
 *  ⚠️ 민간 사이트 배경 수집은 원칙적으로 ToS 주의 대상(§3.3). 사용자가 명시적으로
 *     '전 소스 추가'를 지시했고, 리스크 완화를 위해 **저빈도(6시간 주기)·소수 키워드**로 제한한다.
 *  - 같은 공고(url)는 upsert(갱신만), 이미 마감된 공고는 적재하지 않음.
 *  - 각 소스는 독립 try/catch — 한 소스가 실패(예: 고용24 에러페이지)해도 나머지는 진행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobImportService {

    private final Work24ScraperService work24ScraperService;   // 고용24 (공공 포털)
    private final JobkoreaService jobkoreaService;             // 잡코리아 (민간)
    private final SaraminService saraminService;               // 사람인 (민간, 키 필요)
    private final WorknetService worknetService;               // 워크넷 (공공)
    private final ImportedJobRepository importedJobRepository;
    private final List<JobFeedCollector> jobFeedCollectors;
    private final DiscordNotifierService discordNotifierService; // 신규 공고 Discord 알림

    /** 학과 연관 기본 수집 키워드 (소수로 제한). */
    private static final List<String> KEYWORDS = List.of("IT", "개발", "정보보안", "네트워크");

    private static final long MANUAL_COOLDOWN_MINUTES = 20;   // 수동 트리거 최소 간격
    private static final int EXPIRED_KEEP_DAYS = 7;           // 마감 후 보관 기간

    private volatile LocalDateTime lastImportAt;

    /** 스케줄러용 — 항상 실행. 수집/갱신 건수 반환. */
    @Transactional
    public int importAll() {
        List<ImportedJob> newJobs = new ArrayList<>();
        int count = 0;
        for (String kw : KEYWORDS) {
            try {
                count += importKeyword(kw, newJobs);
            } catch (Exception e) {
                log.warn("[채용수집] 키워드 '{}' 수집 실패: {}", kw, e.getMessage());
            }
        }
        count += importFeeds(newJobs);
        purgeExpired();
        lastImportAt = LocalDateTime.now();
        log.info("[채용수집] 완료 — 신규/갱신 {}건", count);
        try {
            discordNotifierService.notifyNewJobs(newJobs);
        } catch (Exception e) {
            log.warn("[Discord] 알림 처리 실패: {}", e.getMessage());
        }
        return count;
    }

    /** 수동 트리거 — 쿨다운 내면 스킵(레이트리밋). 실제 수집했으면 건수, 스킵이면 -1. */
    @Transactional
    public int refreshIfAllowed() {
        if (lastImportAt != null
                && lastImportAt.isAfter(LocalDateTime.now().minusMinutes(MANUAL_COOLDOWN_MINUTES))) {
            log.info("[채용수집] 수동 트리거 쿨다운 — 스킵");
            return -1;
        }
        return importAll();
    }

    private int importKeyword(String keyword, List<ImportedJob> newJobs) {
        List<JobSearchResult> results = collectFromAllSources(keyword);
        return upsertResults(results, keyword, newJobs);
    }

    /** Sitemap/JSON feeds are collected once per run, not once per search keyword. */
    private int importFeeds(List<ImportedJob> newJobs) {
        int count = 0;
        for (JobFeedCollector collector : jobFeedCollectors) {
            try {
                count += upsertResults(collector.collectLatest(), "feed", newJobs);
            } catch (Exception e) {
                log.warn("[채용수집] {} 피드 수집 실패: {}", collector.source(), e.getMessage());
            }
        }
        return count;
    }

    private int upsertResults(List<JobSearchResult> results, String keyword, List<ImportedJob> newJobs) {
        if (results == null || results.isEmpty()) return 0;
        LocalDate today = LocalDate.now();
        int n = 0;
        for (JobSearchResult r : results) {
            if (r.url() == null || r.url().isBlank()) continue;
            // 이미 마감된 공고는 적재하지 않음
            if (r.deadline() != null && r.deadline().isBefore(today)) continue;

            ImportedJob existing = importedJobRepository.findByUrl(r.url()).orElse(null);
            if (existing != null) {
                existing.refresh(r.title(), r.company(), r.location(),
                        r.deadline(), r.jobType(), r.salary(), keyword);
            } else {
                ImportedJob saved = importedJobRepository.save(ImportedJob.builder()
                        .source(r.source() != null ? r.source() : "work24")
                        .url(r.url())
                        .title(r.title())
                        .company(r.company())
                        .location(r.location())
                        .deadline(r.deadline())
                        .jobType(r.jobType())
                        .salary(r.salary())
                        .keyword(keyword)
                        .build());
                newJobs.add(saved);
            }
            n++;
        }
        return n;
    }

    /** 키워드 1개를 전 소스에서 수집해 합친다. 각 소스 독립 try/catch(한 소스 실패 무시). */
    private List<JobSearchResult> collectFromAllSources(String keyword) {
        List<JobSearchResult> all = new ArrayList<>();
        addSafely(all, "잡코리아", () -> jobkoreaService.searchJobs(keyword, 0, "", "", ""));
        addSafely(all, "고용24",   () -> work24ScraperService.searchJobs(keyword, "", "", "", 1));
        addSafely(all, "사람인",   () -> saraminService.searchJobs(keyword, 0, "", "", ""));
        addSafely(all, "워크넷",   () -> worknetService.searchJobs(keyword, 1));
        return all;
    }

    private void addSafely(List<JobSearchResult> target, String label,
                           java.util.function.Supplier<List<JobSearchResult>> source) {
        try {
            List<JobSearchResult> r = source.get();
            if (r != null) target.addAll(r);
        } catch (Exception e) {
            log.warn("[채용수집] {} 수집 실패: {}", label, e.getMessage());
        }
    }

    private void purgeExpired() {
        long removed = importedJobRepository.deleteByDeadlineBefore(LocalDate.now().minusDays(EXPIRED_KEEP_DAYS));
        if (removed > 0) log.info("[채용수집] 만료 공고 {}건 정리", removed);
    }

    /** 적재된 공고 조회. hideExpired=true 면 지난 공고 제외. */
    @Transactional(readOnly = true)
    public List<ImportedJob> list(String keyword, boolean hideExpired) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        LocalDate minDeadline = hideExpired ? LocalDate.now() : LocalDate.of(1970, 1, 1);
        return importedJobRepository.search(minDeadline, kw);
    }
}
