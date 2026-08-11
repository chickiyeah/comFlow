package com.campusflow.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 제출 답안 1건 (문항별 채점 결과) */
@Entity
@Table(name = "quiz_answers", indexes = @Index(name = "idx_qa_sub", columnList = "submissionId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(nullable = false)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private int awardedScore;

    @Column(nullable = false)
    private boolean aiGraded;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Builder
    public QuizAnswer(Long submissionId, Long questionId, String response,
                      int awardedScore, boolean aiGraded, String feedback) {
        this.submissionId = submissionId;
        this.questionId   = questionId;
        this.response     = response;
        this.awardedScore = awardedScore;
        this.aiGraded     = aiGraded;
        this.feedback     = feedback;
    }

    public void override(int score, String feedback) {
        this.awardedScore = score;
        this.feedback = feedback;
        this.aiGraded = false;   // 교수 검토 완료
    }
}
