package com.campusflow.domain.earlywarning.scheduler;

import com.campusflow.domain.earlywarning.service.EarlyWarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EarlyWarningScheduler {

    private final EarlyWarningService earlyWarningService;

    // 매주 월요일 오전 8시
    @Scheduled(cron = "0 0 8 * * MON")
    public void weeklyCheck() {
        log.info("[조기경보] 주간 학업 위험 점검 시작");
        earlyWarningService.checkAndNotifyAll();
    }
}
