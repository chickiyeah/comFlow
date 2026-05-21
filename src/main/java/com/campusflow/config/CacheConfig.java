package com.campusflow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                // Q-Net (자격증) — 하루에 한 번 정도 갱신
                build("certSchedules",  500, 2,  TimeUnit.HOURS),
                build("qualList",       500, 2,  TimeUnit.HOURS),
                build("qualDetail",     500, 2,  TimeUnit.HOURS),
                build("examLocations",  500, 2,  TimeUnit.HOURS),

                // AI 로드맵 — 직업명 기준 캐시 (1시간)
                build("roadmaps",       200, 1,  TimeUnit.HOURS),

                // AI 응답 캐싱 — AiFacadeService (세맨틱 캐싱 간소화 버전)
                build("aiResponses",    200, 30, TimeUnit.MINUTES),

                // 컴정이 학식 캐시 (2시간)
                build("komjeongMeal",   10,  2,  TimeUnit.HOURS),

                // 통합 캘린더 (10분)
                build("calendarEvents", 100, 10, TimeUnit.MINUTES),

                // 학습 분석 (30분)
                build("gradeTrend",     200, 30, TimeUnit.MINUTES),
                build("attendanceTrend",200, 30, TimeUnit.MINUTES)
        ));
        return manager;
    }

    private CaffeineCache build(String name, int maxSize, long ttl, TimeUnit unit) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttl, unit)
                        .recordStats()
                        .build());
    }
}
