package com.campusflow.domain.review.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "course_reviews")
public class CourseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String subjectName;

    private String professor;
    private int year;
    private int semester;

    @Column(nullable = false)
    private int rating; // 1~5

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean anonymous;
    private LocalDateTime createdAt;

    public CourseReview(Student student, String subjectName, String professor,
                        int year, int semester, int rating, String content, boolean anonymous) {
        this.student     = student;
        this.subjectName = subjectName;
        this.professor   = professor;
        this.year        = year;
        this.semester    = semester;
        this.rating      = Math.max(1, Math.min(5, rating));
        this.content     = content;
        this.anonymous   = anonymous;
        this.createdAt   = LocalDateTime.now();
    }
}
