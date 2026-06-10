package com.campusflow.domain.interview.dto;

import com.campusflow.domain.interview.entity.InterviewQuestion;
import com.campusflow.domain.interview.entity.InterviewSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class InterviewResponse {

    /** 세션 시작 응답 — 첫 번째 질문 포함 */
    @Getter
    @Builder
    public static class SessionStarted {
        private Long sessionId;
        private String company;
        private String position;
        private int totalQuestions;
        private int currentIndex;    // 0
        private String question;     // 첫 질문
    }

    /** 답변 제출 응답 — 다음 질문 or 면접 종료 신호 */
    @Getter
    @Builder
    public static class AnswerResult {
        private Long sessionId;
        private int currentIndex;
        private String feedback;         // 방금 답변에 대한 피드백
        private String nextQuestion;     // null이면 면접 끝
        private boolean finished;
        private String overallFeedback;  // finished=true 시 종합 피드백
    }

    /** 세션 목록 아이템 */
    @Getter
    @Builder
    public static class SessionSummary {
        private Long sessionId;
        private String company;
        private String position;
        private String status;
        private int totalQuestions;
        private int currentIndex;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;

        public static SessionSummary from(InterviewSession s) {
            return SessionSummary.builder()
                    .sessionId(s.getId())
                    .company(s.getCompany())
                    .position(s.getPosition())
                    .status(s.getStatus().name())
                    .totalQuestions(s.getTotalQuestions())
                    .currentIndex(s.getCurrentIndex())
                    .createdAt(s.getCreatedAt())
                    .finishedAt(s.getFinishedAt())
                    .build();
        }
    }

    /** 세션 상세 (결과 조회용) */
    @Getter
    @Builder
    public static class SessionDetail {
        private Long sessionId;
        private String company;
        private String position;
        private String status;
        private String overallFeedback;
        private List<QuestionItem> questions;
        private LocalDateTime createdAt;
        private LocalDateTime finishedAt;

        @Getter
        @Builder
        public static class QuestionItem {
            private int index;
            private String question;
            private String answer;
            private String feedback;

            public static QuestionItem from(InterviewQuestion q) {
                return QuestionItem.builder()
                        .index(q.getQuestionIndex())
                        .question(q.getQuestion())
                        .answer(q.getAnswer())
                        .feedback(q.getFeedback())
                        .build();
            }
        }
    }
}
