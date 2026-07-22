package com.campusflow.domain.material.entity;

import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.storage.entity.StoredFile;
import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 수업 자료. NovaClass {@code materials} 대응. 업로드 파일은 {@link StoredFile}로 연결(선택),
 * 추출된 본문 텍스트는 {@code textContent}에 보관해 AI 요약/튜터/퀴즈의 컨텍스트로 쓴다.
 */
@Entity
@Table(name = "materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private Integer week;

    @Column(length = 100)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String textContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Material(ClassRoom classRoom, String title, String instructions, Integer week, String topic,
                    StoredFile storedFile, String textContent, User createdBy) {
        this.classRoom = classRoom;
        this.title = title;
        this.instructions = instructions;
        this.week = week;
        this.topic = topic;
        this.storedFile = storedFile;
        this.textContent = textContent;
        this.createdBy = createdBy;
    }

    public void update(String title, String instructions, Integer week) {
        this.title = title;
        this.instructions = instructions;
        this.week = week;
    }

    public void updateTopic(String topic) {
        this.topic = topic;
    }
}
