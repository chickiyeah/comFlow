package com.campusflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync   // @Async 활성화 (웹푸시 발송, 이메일 발송 비동기 처리)
public class AppConfig {
    // RestTemplate 빈 제거 — 전 서비스가 RestClient로 통일됨
}
