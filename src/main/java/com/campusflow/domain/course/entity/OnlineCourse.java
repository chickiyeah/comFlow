package com.campusflow.domain.course.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 온라인 강좌(VOD). 교직원이 등록하고 학생이 시청한다. */
@Entity
@Table(name = "online_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlineCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String videoUrl;        // YouTube/직접 URL

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 60)
    private String category;

    @Column(length = 40)
    private String instructorName;  // 등록한 교직원 이름 (표시용)

    private Long createdById;       // 등록자 user id (소유권)

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public OnlineCourse(String title, String description, String videoUrl, String thumbnailUrl,
                        String category, String instructorName, Long createdById, Boolean active) {
        this.title         = title;
        this.description   = description;
        this.videoUrl      = videoUrl;
        this.thumbnailUrl  = thumbnailUrl;
        this.category      = category;
        this.instructorName = instructorName;
        this.createdById   = createdById;
        this.active        = active == null || active;
        this.viewCount     = 0;
        this.createdAt     = LocalDateTime.now();
    }

    public void update(String title, String description, String videoUrl, String thumbnailUrl,
                       String category, Boolean active) {
        if (title != null)        this.title = title;
        if (description != null)  this.description = description;
        if (videoUrl != null)     this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.category = category;
        if (active != null)       this.active = active;
    }

    public void increaseView() { this.viewCount++; }
}
