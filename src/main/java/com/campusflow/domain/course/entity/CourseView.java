package com.campusflow.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 학생의 강좌 시청 기록 (강좌별 1회, 조회수·내가 본 강좌 집계용) */
@Entity
@Table(name = "course_views", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private LocalDateTime watchedAt;

    @Column(nullable = false)
    private boolean completed;      // 수강 완료 여부

    @Column(nullable = false)
    private int lastPositionSec;    // 마지막 시청 위치(초) — 이어보기

    @Column(nullable = false)
    private int durationSec;        // 영상 길이(초) — 진도율 계산용

    @Builder
    public CourseView(Long courseId, Long studentId) {
        this.courseId  = courseId;
        this.studentId = studentId;
        this.watchedAt = LocalDateTime.now();
        this.completed = false;
        this.lastPositionSec = 0;
        this.durationSec = 0;
    }

    public void markCompleted() { this.completed = true; }

    public void updateProgress(int positionSec, int duration) {
        if (positionSec >= 0) this.lastPositionSec = positionSec;
        if (duration > 0) this.durationSec = duration;
        this.watchedAt = LocalDateTime.now();
    }
}
