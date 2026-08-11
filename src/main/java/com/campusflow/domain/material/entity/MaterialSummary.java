package com.campusflow.domain.material.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자료 AI 3단계 요약(한 줄/한 단락/상세). material당 1개(캐시).
 */
@Entity
@Table(name = "material_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private Material material;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String shortSummary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String paragraphSummary;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String detailedSummary;

    @Builder
    public MaterialSummary(Material material, String shortSummary, String paragraphSummary, String detailedSummary) {
        this.material = material;
        this.shortSummary = shortSummary;
        this.paragraphSummary = paragraphSummary;
        this.detailedSummary = detailedSummary;
    }

    public void update(String shortSummary, String paragraphSummary, String detailedSummary) {
        this.shortSummary = shortSummary;
        this.paragraphSummary = paragraphSummary;
        this.detailedSummary = detailedSummary;
    }
}
