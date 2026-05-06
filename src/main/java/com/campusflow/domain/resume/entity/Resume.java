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

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResumePortfolio> resumePortfolios = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Resume(Student student, String title, String summary, String skills, String targetJob) {
        this.student = student;
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
    }

    public void update(String title, String summary, String skills, String targetJob) {
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
    }

    public void clearPortfolios() {
        this.resumePortfolios.clear();
    }
}
