package com.campusflow.domain.interview.dto;

import lombok.Getter;

/** POST /api/interview/sessions/{id}/answer */
@Getter
public class SubmitAnswerRequest {
    private String answer;
}
