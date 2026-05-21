package com.campusflow.domain.study.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "study_groups")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int maxMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_student_id")
    private Student leader;

    @Enumerated(EnumType.STRING)
    private StudyStatus status;

    private LocalDateTime createdAt;

    public StudyGroup(String name, String subject, String description, int maxMembers, Student leader) {
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.maxMembers = maxMembers;
        this.leader = leader;
        this.status = StudyStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public void close()   { this.status = StudyStatus.CLOSED; }
    public void markFull(){ this.status = StudyStatus.FULL; }
    public void reopen()  { this.status = StudyStatus.OPEN; }
}
