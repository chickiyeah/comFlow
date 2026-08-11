package com.campusflow.domain.storage.entity;

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
 * 디스크에 저장된 업로드 파일의 메타데이터. 실제 바이트는 {@code campusflow.storage.root} 아래에 저장되고
 * 여기에는 storageKey(상대경로)와 원본명/타입/크기/소유자만 보관한다.
 */
@Entity
@Table(name = "stored_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 300)
    private String storageKey; // 예: 2026/07/uuid.pdf

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User ownerUser;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StoredFile(String storageKey, String originalFilename, String contentType, long sizeBytes, User ownerUser) {
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.ownerUser = ownerUser;
    }
}
