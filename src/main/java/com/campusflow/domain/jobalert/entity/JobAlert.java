package com.campusflow.domain.jobalert.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "job_alerts")
public class JobAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String keyword;
    private String region;
    private String empType;
    private LocalDateTime lastNotifiedAt;
    private LocalDateTime createdAt;

    public JobAlert(Student student, String keyword, String region, String empType) {
        this.student = student;
        this.keyword = keyword;
        this.region = region;
        this.empType = empType;
        this.createdAt = LocalDateTime.now();
    }

    public void updateLastNotified() {
        this.lastNotifiedAt = LocalDateTime.now();
    }
}
