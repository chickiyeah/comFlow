package com.campusflow.domain.multimodal.dto;

/** 멀티모달 분석 결과. type: document / audio / image / unsupported. */
public record MultimodalResponse(
        String type,
        String answer
) {
}
