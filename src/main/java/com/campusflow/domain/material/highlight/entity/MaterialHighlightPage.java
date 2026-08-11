package com.campusflow.domain.material.highlight.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 분석 대상 페이지의 원문 텍스트(클라이언트 PDF 텍스트레이어 추출본). */
@Entity
@Table(name = "material_highlight_pages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaterialHighlightPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private MaterialHighlightAnalysis analysis;

    @Column(nullable = false)
    private int pageNumber;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String sourceText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String candidatesJson;

    @Builder
    public MaterialHighlightPage(MaterialHighlightAnalysis analysis, int pageNumber, String sourceText) {
        this.analysis = analysis;
        this.pageNumber = pageNumber;
        this.sourceText = sourceText;
    }

    public void setCandidatesJson(String candidatesJson) {
        this.candidatesJson = candidatesJson;
    }
}
