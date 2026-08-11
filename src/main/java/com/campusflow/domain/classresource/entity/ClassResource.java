package com.campusflow.domain.classresource.entity;

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

/** 클래스 자료실 항목(파일 또는 링크). 테이블명은 기존 도메인과 충돌 방지 위해 class_resources. */
@Entity
@Table(name = "class_resources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClassResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ClassResource(ClassRoom classRoom, ResourceType type, String title, String url,
                         StoredFile storedFile, User createdBy) {
        this.classRoom = classRoom;
        this.type = type;
        this.title = title;
        this.url = url;
        this.storedFile = storedFile;
        this.createdBy = createdBy;
    }
}
