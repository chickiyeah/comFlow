package com.campusflow.domain.material.highlight.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 추출된 하이라이트(핵심 구절 + 설명). rectsJson(좌표)는 텍스트 전용 파이프라인에서 null. */
@Entity
@Table(name = "material_pdf_highlights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialPdfHighlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private MaterialHighlightAnalysis analysis;

    @Column(nullable = false)
    private int pageNumber;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(length = 50)
    private String category;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String rectsJson;

    @Column(nullable = false)
    private int displayOrder;

    @Builder
    public MaterialPdfHighlight(MaterialHighlightAnalysis analysis, int pageNumber, String excerpt,
                                String explanation, String category, String rectsJson, int displayOrder) {
        this.analysis = analysis;
        this.pageNumber = pageNumber;
        this.excerpt = excerpt;
        this.explanation = explanation;
        this.category = category;
        this.rectsJson = rectsJson;
        this.displayOrder = displayOrder;
    }
}
