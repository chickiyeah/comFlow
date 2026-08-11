package com.campusflow.domain.quiz.dto;

import java.util.List;

/** 응시용 — 정답/기준은 제외 */
public record QuizDetailResponse(
        Long id,
        String title,
        String description,
        List<QuestionView> questions
) {
    public record QuestionView(
            Long id,
            String type,
            String text,
            List<String> options,
            int points
    ) {}
}
