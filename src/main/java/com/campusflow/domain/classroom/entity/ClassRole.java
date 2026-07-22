package com.campusflow.domain.classroom.entity;

/**
 * 클래스별 멤버 역할. 전역 {@code Role}(ROLE_STUDENT/PROFESSOR/ADMIN)과 무관하며,
 * NovaClass처럼 "클래스를 만든 사람이 그 클래스의 교사"가 되는 클래스 스코프 권한을 표현한다.
 */
public enum ClassRole {
    OWNER,   // 클래스 개설자
    TEACHER, // 공동 교사
    STUDENT  // 학생
}
