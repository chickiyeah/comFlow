package com.campusflow.domain.resume.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ResumeRequest(
        @NotBlank(message = "이력서 제목을 입력해주세요.") String title,
        String summary,
        String skills,
        String targetJob,
        List<Long> portfolioIds // 연동할 포트폴리오 ID 목록 (순서 = 표시 순서)
) {}
