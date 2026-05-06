package com.campusflow.domain.student.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, nullable = false, length = 20)
    private String studentId; // 학번

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private int grade; // 학년 (1 or 2)

    @Column(nullable = false)
    private int semester; // 학기 (1 or 2)

    @Column(nullable = false, length = 50)
    private String department;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Builder
    public Student(User user, String studentId, String name, int grade, int semester,
                   String department, String phone, String email) {
        this.user = user;
        this.studentId = studentId;
        this.name = name;
        this.grade = grade;
        this.semester = semester;
        this.department = department;
        this.phone = phone;
        this.email = email;
    }
}
