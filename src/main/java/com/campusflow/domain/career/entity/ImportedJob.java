package com.campusflow.domain.career.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 백그라운드 스케줄러가 공공 채용 API(고용24 등)에서 주기적으로 수집·적재한 채용공고.
 *
 * §3.3 크롤링 ToS: 민간 사이트(잡코리아·사람인)는 배경 수집 대상에서 제외하고
 * 공공/정부 채용 포털만 적재한다. 같은 공고(url) 재수집 시 upsert(갱신만).
 * §3.4 데이터 안전: additive 테이블 — 기존 스키마 무손상.
 *
 * '지난 공고' 필터: deadline 이 지난 건은 조회 시 제외(ImportedJobRepository.search),
 * 일정 기간 지난 건은 스케줄러가 purge.
 */
@Entity
@Table(name = "imported_jobs", indexes = {
        @Index(name = "idx_imported_jobs_deadline", columnList = "deadline")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ImportedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String source;          // 'work24' 등 공공 출처

    @Column(length = 500)
    private String url;             // 중복 판정 키

    @Column(length = 300)
    private String title;

    @Column(length = 200)
    private String company;

    @Column(length = 200)
    private String location;

    private LocalDate deadline;     // null = 상시/채용시 마감 (만료로 보지 않음)

    @Column(length = 100)
    private String jobType;

    @Column(length = 100)
    private String salary;

    @Column(length = 100)
    private String keyword;         // 어떤 검색어로 수집됐는지

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime importedAt;

    @LastModifiedDate
    private LocalDateTime lastSeenAt;

    @Builder
    public ImportedJob(String source, String url, String title, String company, String location,
                       LocalDate deadline, String jobType, String salary, String keyword) {
        this.source = source;
        this.url = url;
        this.title = title;
        this.company = company;
        this.location = location;
        this.deadline = deadline;
        this.jobType = jobType;
        this.salary = salary;
        this.keyword = keyword;
    }

    /** 재수집 시 변동 가능한 필드만 갱신(upsert) — lastSeenAt 자동 갱신. */
    public void refresh(String title, String company, String location,
                        LocalDate deadline, String jobType, String salary, String keyword) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.deadline = deadline;
        this.jobType = jobType;
        this.salary = salary;
        this.keyword = keyword;
    }
}
