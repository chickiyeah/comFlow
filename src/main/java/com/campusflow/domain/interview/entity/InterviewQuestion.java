package com.campusflow.domain.interview.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interview_questions",
       indexes = @Index(name = "idx_iq_session", columnList = "session_id, question_index"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    /** 0-based 질문 순번 */
    @Column(nullable = false)
    private int questionIndex;

    /** AI가 생성한 질문 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    /** 학생의 답변 (제출 전 null) */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /** AI가 생성한 이 질문에 대한 단건 피드백 */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Builder
    public InterviewQuestion(InterviewSession session, int questionIndex, String question) {
        this.session = session;
        this.questionIndex = questionIndex;
        this.question = question;
    }

    public void submitAnswer(String answer) {
        this.answer = answer;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
