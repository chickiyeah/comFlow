package com.campusflow.domain.kmate.dto;

import java.util.List;

/** TOPIK 연습 객관식 문항. correctAnswer는 options의 0부터 시작하는 인덱스(문자열). */
public record KmateQuizQuestion(
        String text,
        List<String> options,
        String correctAnswer,
        String explanation
) {
}
