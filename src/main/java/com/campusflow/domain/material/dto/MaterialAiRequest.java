package com.campusflow.domain.material.dto;

/**
 * 자료별 AI 액션 요청.
 * action: {@code chat}(레벨별 튜터) / {@code quiz}(자료 기반 퀴즈 생성).
 * message: chat일 때 질문. level: chat 난이도(초급/중급/고급, 기본 중급).
 */
public record MaterialAiRequest(
        String action,
        String message,
        String level
) {
}
