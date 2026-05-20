package com.campusflow.domain.notice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean important;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Notice(String title, String summary, String content, boolean important) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.important = important;
        this.createdAt = LocalDateTime.now();
    }
}
