package com.campusflow.domain.quiz.dto;

import java.util.List;

public record QuizResultResponse(
        int score,
        int maxScore,
        boolean needsReview,
        List<AnswerResult> answers
) {
    public record AnswerResult(
            Long questionId,
            int awardedScore,
            int points,
            boolean aiGraded,
            String feedback
    ) {}
}
