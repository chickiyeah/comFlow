package com.campusflow.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 학생의 퀴즈 제출/채점 결과. needsReview=true면 단답 AI채점 포함 → 교수 검토 권장. */
@Entity
@Table(name = "quiz_submissions", indexes = @Index(name = "idx_qs_quiz", columnList = "quizId, studentId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long quizId;

    @Column(nullable = false)
    private Long studentId;

    @Column(length = 40)
    private String studentName;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private boolean needsReview;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Builder
    public QuizSubmission(Long quizId, Long studentId, String studentName, int score, int maxScore, boolean needsReview) {
        this.quizId      = quizId;
        this.studentId   = studentId;
        this.studentName = studentName;
        this.score       = score;
        this.maxScore    = maxScore;
        this.needsReview = needsReview;
        this.submittedAt = LocalDateTime.now();
    }

    public void applyScore(int score, boolean needsReview) {
        this.score = score;
        this.needsReview = needsReview;
    }
}
