package com.campusflow.domain.interview.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** 지원 회사명 */
    @Column(nullable = false, length = 100)
    private String company;

    /** 지원 직무 / 공고 제목 */
    @Column(nullable = false, length = 200)
    private String position;

    /** 총 질문 수 (기본 5개) */
    @Column(nullable = false)
    private int totalQuestions;

    /** 현재까지 진행된 질문 번호 (0-based) */
    @Column(nullable = false)
    private int currentIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    /** 면접 종료 후 AI 종합 피드백 */
    @Column(columnDefinition = "TEXT")
    private String overallFeedback;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    @Builder
    public InterviewSession(Student student, String company, String position, int totalQuestions) {
        this.student = student;
        this.company = company;
        this.position = position;
        this.totalQuestions = totalQuestions;
        this.currentIndex = 0;
        this.status = InterviewStatus.IN_PROGRESS;
    }

    public void nextQuestion() {
        this.currentIndex++;
    }

    public void finish(String overallFeedback) {
        this.status = InterviewStatus.FINISHED;
        this.overallFeedback = overallFeedback;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isFinished() {
        return this.currentIndex >= this.totalQuestions;
    }
}
