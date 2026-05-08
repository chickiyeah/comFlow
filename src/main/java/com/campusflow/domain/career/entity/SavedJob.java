package com.campusflow.domain.career.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String company;

    @Column(length = 50)
    private String location;

    @Column(length = 300)
    private String url;

    @Column
    private LocalDate deadline;

    @Column(length = 100)
    private String jobType;

    @Column(length = 100)
    private String salary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String source;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime savedAt;

    @Builder
    public SavedJob(Student student, String title, String company, String location,
                    String url, LocalDate deadline, String jobType, String salary,
                    String description, String source) {
        this.student = student;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.deadline = deadline;
        this.jobType = jobType;
        this.salary = salary;
        this.description = description;
        this.source = source;
    }
}
