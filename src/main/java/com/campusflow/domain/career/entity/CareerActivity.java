package com.campusflow.domain.career.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "career_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CareerActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String organization;

    @Column
    private LocalDate targetDate;

    @Column
    private LocalDate completedDate;

    @Column(length = 50)
    private String score;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public CareerActivity(Student student, ActivityType type, ActivityStatus status,
                          String title, String organization, LocalDate targetDate,
                          LocalDate completedDate, String score, String memo) {
        this.student = student;
        this.type = type;
        this.status = status;
        this.title = title;
        this.organization = organization;
        this.targetDate = targetDate;
        this.completedDate = completedDate;
        this.score = score;
        this.memo = memo;
    }

    public void update(ActivityType type, ActivityStatus status, String title,
                       String organization, LocalDate targetDate, LocalDate completedDate,
                       String score, String memo) {
        this.type = type;
        this.status = status;
        this.title = title;
        this.organization = organization;
        this.targetDate = targetDate;
        this.completedDate = completedDate;
        this.score = score;
        this.memo = memo;
    }
}
