package com.campusflow.domain.resume.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String title; // 이력서 제목 (예: 백엔드 개발자 지원)

    @Column(columnDefinition = "TEXT")
    private String summary; // 자기소개

    @Column(length = 255)
    private String skills; // 보유 기술 (쉼표 구분)

    @Column(length = 255)
    private String targetJob; // 희망 직무

    @Column(columnDefinition = "TEXT")
    private String resumeData;   // ResumeData JSON

    @Column(length = 20)
    private String template;     // dev|ncs|general|startup|english|internship

    private Long sourceJobId;    // C 단계 원본 공고 id (nullable)

    @Column(length = 10)
    private String sourceJobType; // saved | imported (nullable)

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumePortfolio> resumePortfolios = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Resume(Student student, String title, String summary, String skills, String targetJob,
                  String resumeData, String template) {
        this.student = student;
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
        this.resumeData = resumeData;
        this.template = template;
    }

    public void update(String title, String summary, String skills, String targetJob) {
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
    }

    public void updateRich(String title, String summary, String skills, String targetJob,
                           String resumeData, String template) {
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
        this.resumeData = resumeData;
        if (template != null && !template.isBlank()) this.template = template;
    }

    public void setSourceJob(Long sourceJobId, String sourceJobType) {
        this.sourceJobId = sourceJobId;
        this.sourceJobType = sourceJobType;
    }

    public void clearPortfolios() {
        this.resumePortfolios.clear();
    }
}
