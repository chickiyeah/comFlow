package com.campusflow.domain.award.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "awards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Award {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String title;          // 수상명

    @Column(nullable = false, length = 100)
    private String organization;   // 주관 기관

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AwardLevel level;      // 금/은/동/장려/수상

    @Column(nullable = false)
    private LocalDate awardDate;

    @Column(length = 300)
    private String description;

    @Builder
    public Award(Student student, String title, String organization,
                 AwardLevel level, LocalDate awardDate, String description) {
        this.student = student;
        this.title = title;
        this.organization = organization;
        this.level = level;
        this.awardDate = awardDate;
        this.description = description;
    }
}
