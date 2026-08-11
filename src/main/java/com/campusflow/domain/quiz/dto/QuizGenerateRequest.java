package com.campusflow.domain.quiz.dto;

import jakarta.validation.constraints.NotBlank;

/** AI 자동 퀴즈 생성 요청 (초안 생성 — 저장 X). */
public record QuizGenerateRequest(
        Long courseId,
        @NotBlank String topic,
        Integer count,      // 문항 수 (기본 5, 최대 10)
        String type         // MCQ | SHORT | MIX (기본 MIX)
) {}
