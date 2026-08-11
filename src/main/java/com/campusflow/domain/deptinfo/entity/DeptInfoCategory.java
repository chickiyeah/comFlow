package com.campusflow.domain.deptinfo.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** 학과 내부정보 분류. terms = 사용자 질문에서 이 분류로 매칭할 키워드들. */
@Getter
@RequiredArgsConstructor
public enum DeptInfoCategory {
    ADMISSION("입시", List.of("입시", "입학", "전형", "모집", "지원자격", "원서")),
    FACULTY("교수진", List.of("교수", "교수진", "교수님", "담당교수", "지도교수", "선생님")),
    SCHOLARSHIP("장학", List.of("장학", "장학금", "등록금", "학자금")),
    CURRICULUM("교육과정", List.of("교육과정", "커리큘럼", "과목", "수업", "전공")),
    GENERAL("일반", List.of());

    private final String label;
    private final List<String> terms;
}
