package com.campusflow.domain.notification.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_student", columnList = "student_id, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 500)
    private String link;            // 클릭 시 이동할 라우트/URL (선택)

    @Column(name = "is_read", nullable = false)
    private boolean readFlag;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Notification(Student student, NotificationType type, String title, String body, String link) {
        this.student   = student;
        this.type      = type;
        this.title     = title;
        this.body      = body;
        this.link      = link;
        this.readFlag  = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        this.readFlag = true;
    }
}
