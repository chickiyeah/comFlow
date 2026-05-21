package com.campusflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {
    // RestTemplate 빈 제거 — 전 서비스가 RestClient로 통일됨
}
