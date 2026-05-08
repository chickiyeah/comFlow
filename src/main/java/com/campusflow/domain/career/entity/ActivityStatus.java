package com.campusflow.domain.career.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityStatus {
    PLANNING("예정"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    FAILED("불합격/취소");

    private final String label;
}
