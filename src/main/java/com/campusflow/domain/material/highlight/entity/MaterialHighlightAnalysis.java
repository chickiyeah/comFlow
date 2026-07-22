package com.campusflow.domain.material.highlight.entity;

import com.campusflow.domain.material.entity.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** PDF 스마트 하이라이트 분석 작업. 자료당 최신 1건을 현재 분석으로 취급. fingerprint로 재계산 방지. */
@Entity
@Table(name = "material_highlight_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MaterialHighlightAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HighlightStatus status;

    @Column(length = 80)
    private String fingerprint;

    private int totalPages;

    private int completedPages;

    @Column(length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public MaterialHighlightAnalysis(Material material, HighlightStatus status, String fingerprint, int totalPages) {
        this.material = material;
        this.status = status;
        this.fingerprint = fingerprint;
        this.totalPages = totalPages;
        this.completedPages = 0;
    }

    public void markProcessing() {
        this.status = HighlightStatus.PROCESSING;
    }

    public void incrementCompleted() {
        this.completedPages++;
    }

    public void markReady() {
        this.status = HighlightStatus.READY;
    }

    public void markFailed(String message) {
        this.status = HighlightStatus.FAILED;
        this.errorMessage = message != null && message.length() > 500 ? message.substring(0, 500) : message;
    }

    public void resetForRetry() {
        this.status = HighlightStatus.PROCESSING;
        this.completedPages = 0;
        this.errorMessage = null;
    }
}
