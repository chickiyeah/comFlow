package com.campusflow.domain.material.highlight.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 클라이언트가 PDF 텍스트레이어에서 추출한 페이지 본문을 제출. */
public record HighlightStartRequest(
        @NotNull(message = "페이지 목록이 필요합니다.")
        List<PageInput> pages
) {
    public record PageInput(Integer pageNumber, String text) {}
}
