package com.campusflow.domain.kmate.dto;

import java.util.List;

public record KmateQuizCheckResponse(
        int score,
        int total,
        List<ItemResult> results
) {
    public record ItemResult(String question, boolean correct, String correctAnswer, String userAnswer) {}
}
