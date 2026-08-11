package com.campusflow.domain.classattendance.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 세션별 학생 출결 기록. (session, student) 유일. */
@Entity
@Table(name = "class_attendance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_attendance", columnNames = {"session_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassAttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassAttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassAttendanceStatus status;

    private LocalDateTime markedAt;

    @Builder
    public ClassAttendanceRecord(ClassAttendanceSession session, User student, ClassAttendanceStatus status,
                                 LocalDateTime markedAt) {
        this.session = session;
        this.student = student;
        this.status = status;
        this.markedAt = markedAt;
    }

    public void mark(ClassAttendanceStatus status, LocalDateTime markedAt) {
        this.status = status;
        this.markedAt = markedAt;
    }
}
