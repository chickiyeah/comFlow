package com.campusflow.domain.jobalert.scheduler;

import com.campusflow.domain.jobalert.service.JobAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobAlertScheduler {

    private final JobAlertService jobAlertService;

    // 매일 오전 9시
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyJobAlert() {
        log.info("[채용알리미] 일일 채용공고 알림 발송 시작");
        jobAlertService.runDailyAlert();
    }
}
