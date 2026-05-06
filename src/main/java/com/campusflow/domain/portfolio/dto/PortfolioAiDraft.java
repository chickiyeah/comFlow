package com.campusflow.domain.portfolio.dto;

import com.campusflow.domain.portfolio.entity.PortfolioStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * AI가 분석해서 반환하는 포트폴리오 초안.
 * 프론트에서 수정 후 POST /api/portfolios 로 저장.
 */
public record PortfolioAiDraft(
        String title,
        String description,
        String role,
        List<String> techStack,
        LocalDate startDate,
        LocalDate endDate,
        String githubUrl,
        String deployUrl,
        PortfolioStatus status,
        String source       // "GITHUB" | "FILE"
) {}
