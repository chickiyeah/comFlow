package com.campusflow.domain.attendance.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 50)
    private String subjectName;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceStatus status;

    @Column(length = 100)
    private String note;

    @Builder
    public Attendance(Student student, String subjectName, LocalDate attendanceDate,
                      AttendanceStatus status, String note) {
        this.student = student;
        this.subjectName = subjectName;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.note = note;
    }
}
