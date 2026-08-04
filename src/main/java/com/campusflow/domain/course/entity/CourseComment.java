package com.campusflow.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 강좌 Q&A 댓글. parentId 가 있으면 답글. AI 조교 답변은 aiGenerated=true. */
@Entity
@Table(name = "course_comments", indexes = @Index(name = "idx_comment_course", columnList = "courseId, createdAt"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    private Long parentId;          // 답글이면 부모 댓글 id

    private Long authorUserId;      // AI 답변이면 null

    @Column(nullable = false, length = 40)
    private String authorName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean question;       // 질문 여부

    @Column(nullable = false)
    private boolean staff;          // 교직원 작성 여부

    @Column(nullable = false)
    private boolean aiGenerated;    // AI 조교 답변 여부

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public CourseComment(Long courseId, Long parentId, Long authorUserId, String authorName,
                         String content, boolean question, boolean staff, boolean aiGenerated) {
        this.courseId     = courseId;
        this.parentId     = parentId;
        this.authorUserId = authorUserId;
        this.authorName   = authorName;
        this.content      = content;
        this.question     = question;
        this.staff        = staff;
        this.aiGenerated  = aiGenerated;
        this.createdAt    = LocalDateTime.now();
    }
}
