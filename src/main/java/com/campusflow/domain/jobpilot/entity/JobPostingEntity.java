package com.campusflow.domain.jobpilot.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 수집·추출한 채용 공고 원문/결과 보관 (캐시 + 중복 방지).
 * §3.3 크롤링 ToS: 같은 공고(url) 재요청 시 이 테이블의 캐시를 사용한다.
 * §3.4 데이터 안전: additive 테이블 — 기존 스키마를 건드리지 않는다.
 */
@Entity
@Table(name = "jobpilot_postings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class JobPostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(length = 20)
    private String source;          // 'url' | 'paste'

    @Column(length = 500)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String extractedJson;   // JobPosting 직렬화 (추출 후 채워짐)

    @Column(length = 100)
    private String company;

    @Column(length = 100)
    private String position;

    @Column(length = 100)
    private String deadline;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public JobPostingEntity(Student student, String source, String url, String rawText) {
        this.student = student;
        this.source = source;
        this.url = url;
        this.rawText = rawText;
    }

    /** 추출 결과를 채운다(additive 업데이트). */
    public void applyExtraction(String extractedJson, String company, String position, String deadline) {
        this.extractedJson = extractedJson;
        this.company = company;
        this.position = position;
        this.deadline = deadline;
    }
}
