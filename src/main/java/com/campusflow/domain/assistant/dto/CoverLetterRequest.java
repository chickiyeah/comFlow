package com.campusflow.domain.assistant.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CoverLetterRequest(
        @NotBlank(message = "회사명을 입력해주세요.") String companyName,
        @NotBlank(message = "직무를 입력해주세요.") String jobTitle,
        List<Long> portfolioIds,
        List<String> sections   // 소제목 목록 (optional, 예: ["지원동기", "직무역량"])
) {}
