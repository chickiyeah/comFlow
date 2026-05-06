package com.campusflow.domain.portfolio.dto;

import com.campusflow.domain.portfolio.entity.PortfolioStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PortfolioRequest(
        @NotBlank(message = "프로젝트 제목을 입력해주세요.") String title,
        String description,
        @NotBlank(message = "역할을 입력해주세요.") String role,
        String techStack,
        LocalDate startDate,
        LocalDate endDate,
        String githubUrl,
        String deployUrl,
        @NotNull(message = "상태를 선택해주세요.") PortfolioStatus status
) {}
