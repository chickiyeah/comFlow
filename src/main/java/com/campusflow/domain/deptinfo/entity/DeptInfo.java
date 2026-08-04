package com.campusflow.domain.deptinfo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 학과 내부정보(입시·교수진·장학 등). 컴정이 챗에서 AI 답변 근거로 주입된다. */
@Entity
@Table(name = "dept_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeptInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeptInfoCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 255)
    private String keywords;        // 추가 매칭 키워드 (쉼표 구분, 선택)

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public DeptInfo(DeptInfoCategory category, String title, String content, String keywords, Boolean active) {
        this.category  = category;
        this.title     = title;
        this.content   = content;
        this.keywords  = keywords;
        this.active    = active == null || active;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(DeptInfoCategory category, String title, String content, String keywords, Boolean active) {
        if (category != null) this.category = category;
        if (title != null)    this.title = title;
        if (content != null)  this.content = content;
        this.keywords = keywords;
        if (active != null)   this.active = active;
        this.updatedAt = LocalDateTime.now();
    }
}
