package com.campusflow.domain.progress.dto;

/** 클래스룸 대시보드 요약(read-model). */
public record ProgressSummaryResponse(
        long classesJoined,
        long classesTeaching,
        long kmateQuestions,
        long materials
) {
}
