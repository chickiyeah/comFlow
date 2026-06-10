package com.campusflow.domain.interview.dto;

import lombok.Getter;

/** POST /api/interview/sessions */
@Getter
public class StartInterviewRequest {
    private Long savedJobId;       // null이면 직접 입력
    private String company;        // savedJobId null 시 직접 입력
    private String position;
    private int totalQuestions;    // 기본 5, 최대 10
}
