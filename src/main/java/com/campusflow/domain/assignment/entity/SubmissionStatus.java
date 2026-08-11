package com.campusflow.domain.assignment.entity;

public enum SubmissionStatus {
    TURNED_IN, // 제출됨(기한 내)
    LATE,      // 지각 제출
    GRADED,    // 채점 완료
    RETURNED   // 반려(재제출 요청)
}
