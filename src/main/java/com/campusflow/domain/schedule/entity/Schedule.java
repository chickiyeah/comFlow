package com.campusflow.domain.schedule.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String subjectName;

    private String subjectCode;
    private String professor;
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private String startTime; // "09:00"

    @Column(nullable = false)
    private String endTime;   // "10:30"

    private int year;
    private int semester;

    public Schedule(Student student, String subjectName, String subjectCode,
                    String professor, String room, DayOfWeek dayOfWeek,
                    String startTime, String endTime, int year, int semester) {
        this.student = student;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.professor = professor;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.year = year;
        this.semester = semester;
    }

    public void update(String subjectName, String subjectCode, String professor,
                       String room, DayOfWeek dayOfWeek, String startTime,
                       String endTime, int year, int semester) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.professor = professor;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.year = year;
        this.semester = semester;
    }
}
