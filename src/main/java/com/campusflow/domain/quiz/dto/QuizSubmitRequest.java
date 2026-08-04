package com.campusflow.domain.quiz.dto;

import java.util.List;

public record QuizSubmitRequest(
        List<AnswerReq> answers
) {
    public record AnswerReq(Long questionId, String response) {}
}
