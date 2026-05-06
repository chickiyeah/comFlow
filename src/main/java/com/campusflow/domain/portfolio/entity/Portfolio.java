package com.campusflow.domain.portfolio.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String role; // 역할 (팀장, 백엔드 개발 등)

    @Column(length = 255)
    private String techStack; // 쉼표 구분 (Java, Spring Boot, MySQL)

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(length = 255)
    private String githubUrl;

    @Column(length = 255)
    private String deployUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PortfolioStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Portfolio(Student student, String title, String description, String role,
                     String techStack, LocalDate startDate, LocalDate endDate,
                     String githubUrl, String deployUrl, PortfolioStatus status) {
        this.student = student;
        this.title = title;
        this.description = description;
        this.role = role;
        this.techStack = techStack;
        this.startDate = startDate;
        this.endDate = endDate;
        this.githubUrl = githubUrl;
        this.deployUrl = deployUrl;
        this.status = status;
    }

    public void update(String title, String description, String role, String techStack,
                       LocalDate startDate, LocalDate endDate, String githubUrl,
                       String deployUrl, PortfolioStatus status) {
        this.title = title;
        this.description = description;
        this.role = role;
        this.techStack = techStack;
        this.startDate = startDate;
        this.endDate = endDate;
        this.githubUrl = githubUrl;
        this.deployUrl = deployUrl;
        this.status = status;
    }
}
