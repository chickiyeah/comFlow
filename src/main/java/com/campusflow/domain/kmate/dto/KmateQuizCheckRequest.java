package com.campusflow.domain.kmate.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 생성된 문항을 사용자 답안과 함께 되돌려 채점 요청. */
public record KmateQuizCheckRequest(
        @NotNull(message = "채점할 항목이 필요합니다.")
        List<Item> items
) {
    public record Item(String question, String correctAnswer, String userAnswer) {}
}
