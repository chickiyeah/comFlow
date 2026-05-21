package com.campusflow.domain.study.entity;

import com.campusflow.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "study_members", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id","student_id"}))
public class StudyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private LocalDateTime joinedAt;

    public StudyMember(StudyGroup group, Student student) {
        this.group = group;
        this.student = student;
        this.joinedAt = LocalDateTime.now();
    }
}
