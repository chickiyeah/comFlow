package com.campusflow.domain.assignment.entity;

import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 과제 제출물. (assignment, student) 유일. */
@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission", columnNames = {"assignment_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    private Integer grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private LocalDateTime submittedAt;

    private LocalDateTime gradedAt;

    @Builder
    public Submission(Assignment assignment, User student, String content, StoredFile storedFile,
                      SubmissionStatus status, LocalDateTime submittedAt) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.storedFile = storedFile;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    /** 재제출 — 채점 결과 초기화. */
    public void resubmit(String content, StoredFile storedFile, SubmissionStatus status, LocalDateTime submittedAt) {
        this.content = content;
        this.storedFile = storedFile;
        this.status = status;
        this.submittedAt = submittedAt;
        this.grade = null;
        this.feedback = null;
        this.gradedAt = null;
    }

    public void grade(int grade, String feedback, LocalDateTime gradedAt) {
        this.grade = grade;
        this.feedback = feedback;
        this.status = SubmissionStatus.GRADED;
        this.gradedAt = gradedAt;
    }

    public void markReturned() {
        this.status = SubmissionStatus.RETURNED;
    }
}
