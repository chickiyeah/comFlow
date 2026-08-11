package com.campusflow.domain.gamification.dto;

import java.util.List;

/** 학생 성취(포인트·레벨·배지) — 기존 활동 데이터에서 실시간 집계. */
public record GamificationResponse(
        int points,
        int level,
        int levelProgress,     // 현재 레벨 진행 %
        int toNextLevel,       // 다음 레벨까지 남은 포인트
        Stats stats,
        List<Badge> badges
) {
    public record Stats(int courseCompletions, int quizzesTaken, int perfectQuizzes,
                        int activities, int awards, double gpa) {}
    public record Badge(String code, String icon, boolean earned) {}
}
