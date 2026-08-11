package com.campusflow.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 퀴즈 문항. MCQ는 optionsJson(보기 JSON)+correctAnswer(정답 인덱스), SHORT는 correctAnswer(모범답안/채점기준). */
@Entity
@Table(name = "quiz_questions", indexes = @Index(name = "idx_qq_quiz", columnList = "quizId, seq"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long quizId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(columnDefinition = "TEXT")
    private String optionsJson;     // MCQ 보기 JSON 배열

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;   // MCQ: 정답 인덱스 / SHORT: 모범답안·채점기준

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int seq;

    @Builder
    public QuizQuestion(Long quizId, QuestionType type, String text, String optionsJson,
                        String correctAnswer, int points, int seq) {
        this.quizId        = quizId;
        this.type          = type;
        this.text          = text;
        this.optionsJson   = optionsJson;
        this.correctAnswer = correctAnswer;
        this.points        = Math.max(1, points);
        this.seq           = seq;
    }
}
