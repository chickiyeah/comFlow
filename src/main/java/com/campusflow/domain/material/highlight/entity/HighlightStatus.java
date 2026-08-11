package com.campusflow.domain.material.highlight.entity;

public enum HighlightStatus {
    PENDING,    // 생성됨(처리 전)
    PROCESSING, // 분석 중
    READY,      // 완료
    FAILED      // 실패
}
