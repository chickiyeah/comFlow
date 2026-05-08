package com.campusflow.domain.coverletter.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cover_letters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CoverLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String jobTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public CoverLetter(Student student, String title, String companyName,
                        String jobTitle, String content) {
        this.student = student;
        this.title = title;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.content = content;
    }

    public void update(String title, String companyName, String jobTitle, String content) {
        this.title = title;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.content = content;
    }
}
