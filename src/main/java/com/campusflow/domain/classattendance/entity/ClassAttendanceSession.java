package com.campusflow.domain.classattendance.entity;

import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 클래스별 출석 세션(회차). 기존 학과 attendance 도메인과 완전히 분리된 별도 테이블. */
@Entity
@Table(name = "class_attendance_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClassAttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by")
    private User openedBy;

    @Column(nullable = false)
    private boolean active;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ClassAttendanceSession(ClassRoom classRoom, String title, LocalDate date, User openedBy, boolean active) {
        this.classRoom = classRoom;
        this.title = title;
        this.date = date;
        this.openedBy = openedBy;
        this.active = active;
    }

    public void close() {
        this.active = false;
    }
}
