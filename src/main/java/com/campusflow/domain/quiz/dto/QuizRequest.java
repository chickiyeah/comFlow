package com.campusflow.domain.quiz.dto;

import com.campusflow.domain.quiz.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuizRequest(
        Long courseId,
        @NotBlank String title,
        String description,
        Boolean active,
        @NotEmpty List<QuestionReq> questions
) {
    public record QuestionReq(
            QuestionType type,
            @NotBlank String text,
            List<String> options,   // MCQ 보기
            String correctAnswer,   // MCQ: 정답 인덱스(문자열) / SHORT: 모범답안·기준
            Integer points
    ) {}
}
