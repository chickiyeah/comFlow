package com.campusflow.domain.grade.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 50)
    private String subjectName; // 과목명

    @Column(nullable = false, length = 20)
    private String subjectCode; // 과목코드

    @Column(nullable = false)
    private int credits; // 학점

    @Column(nullable = false, length = 5)
    private String letterGrade; // A+, A0, B+ ...

    @Column(nullable = false)
    private double gradePoint; // 4.5 환산

    @Column(nullable = false)
    private int gradeYear; // 이수 연도

    @Column(nullable = false)
    private int gradeSemester; // 이수 학기

    @Builder
    public Grade(Student student, String subjectName, String subjectCode, int credits,
                 String letterGrade, double gradePoint, int gradeYear, int gradeSemester) {
        this.student = student;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.credits = credits;
        this.letterGrade = letterGrade;
        this.gradePoint = gradePoint;
        this.gradeYear = gradeYear;
        this.gradeSemester = gradeSemester;
    }
}
