package com.campusflow.domain.career.scheduler;

import com.campusflow.domain.career.service.JobImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공공 채용공고 백그라운드 수집 스케줄러.
 * 기동 30초 후 1회, 이후 6시간 주기로 고용24에서 수집·적재(JobImportService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobImportScheduler {

    private final JobImportService jobImportService;

    private static final long SIX_HOURS_MS = 6L * 60 * 60 * 1000;

    @Scheduled(initialDelay = 30_000, fixedDelay = SIX_HOURS_MS)
    public void scheduledImport() {
        try {
            log.info("[채용수집] 스케줄 수집 시작");
            jobImportService.importAll();
        } catch (Exception e) {
            log.warn("[채용수집] 스케줄 수집 실패: {}", e.getMessage());
        }
    }
}
