package com.campusflow.domain.roadmap.dto;

import jakarta.validation.constraints.NotBlank;

public record RoadmapRequest(
        @NotBlank(message = "직업명을 입력해주세요.") String jobTitle,
        boolean useExternalAi // true = Claude API, false = 학과 내부 RAG만
) {}
