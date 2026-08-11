package com.campusflow.domain.assignment.entity;

import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** 과제. NovaClass {@code assignments} 대응. draft=true면 학생에게 노출되지 않음. */
@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private LocalDateTime dueDate;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private boolean draft;

    @Column(length = 100)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Assignment(ClassRoom classRoom, String title, String instructions, LocalDateTime dueDate,
                      int points, boolean draft, String topic, User createdBy) {
        this.classRoom = classRoom;
        this.title = title;
        this.instructions = instructions;
        this.dueDate = dueDate;
        this.points = points;
        this.draft = draft;
        this.topic = topic;
        this.createdBy = createdBy;
    }

    public void update(String title, String instructions, LocalDateTime dueDate, int points) {
        this.title = title;
        this.instructions = instructions;
        this.dueDate = dueDate;
        this.points = points;
    }

    public void updateDraft(boolean draft) {
        this.draft = draft;
    }

    public void updateTopic(String topic) {
        this.topic = topic;
    }
}
