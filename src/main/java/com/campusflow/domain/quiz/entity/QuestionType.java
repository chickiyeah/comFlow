package com.campusflow.domain.quiz.entity;

/** 문항 유형 — 객관식(자동채점) / 단답(AI 보조채점) */
public enum QuestionType {
    MCQ,    // 객관식
    SHORT   // 단답/서술 (AI 채점)
}
