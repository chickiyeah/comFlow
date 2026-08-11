package com.campusflow.domain.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 퀴즈/시험. 강좌(courseId)에 연결되거나 독립 운영. 교직원이 생성. */
@Entity
@Table(name = "quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long courseId;          // 연결 강좌 (선택)

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long createdById;

    @Column(length = 40)
    private String instructorName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Quiz(Long courseId, String title, String description, Long createdById,
                String instructorName, Boolean active) {
        this.courseId       = courseId;
        this.title          = title;
        this.description    = description;
        this.createdById    = createdById;
        this.instructorName = instructorName;
        this.active         = active == null || active;
        this.createdAt      = LocalDateTime.now();
    }

    public void update(String title, String description, Boolean active) {
        if (title != null)       this.title = title;
        if (description != null) this.description = description;
        if (active != null)      this.active = active;
    }
}
