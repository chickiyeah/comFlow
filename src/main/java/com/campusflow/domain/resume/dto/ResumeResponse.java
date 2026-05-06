package com.campusflow.domain.resume.dto;

import com.campusflow.domain.portfolio.dto.PortfolioResponse;
import com.campusflow.domain.resume.entity.Resume;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ResumeResponse(
        Long id,
        String title,
        String summary,
        List<String> skills,
        String targetJob,
        List<PortfolioResponse> portfolios,
        LocalDateTime updatedAt
) {
    public static ResumeResponse from(Resume resume) {
        List<String> skillList = (resume.getSkills() != null && !resume.getSkills().isBlank())
                ? Arrays.stream(resume.getSkills().split(",")).map(String::trim).toList()
                : List.of();

        List<PortfolioResponse> portfolios = resume.getResumePortfolios().stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(rp -> PortfolioResponse.from(rp.getPortfolio()))
                .toList();

        return new ResumeResponse(
                resume.getId(), resume.getTitle(), resume.getSummary(),
                skillList, resume.getTargetJob(), portfolios, resume.getUpdatedAt()
        );
    }
}
