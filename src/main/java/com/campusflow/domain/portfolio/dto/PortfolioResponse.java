package com.campusflow.domain.portfolio.dto;

import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.entity.PortfolioStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record PortfolioResponse(
        Long id,
        String title,
        String description,
        String role,
        List<String> techStack,
        LocalDate startDate,
        LocalDate endDate,
        String githubUrl,
        String deployUrl,
        PortfolioStatus status
) {
    public static PortfolioResponse from(Portfolio p) {
        List<String> stack = (p.getTechStack() != null && !p.getTechStack().isBlank())
                ? Arrays.stream(p.getTechStack().split(",")).map(String::trim).toList()
                : List.of();
        return new PortfolioResponse(
                p.getId(), p.getTitle(), p.getDescription(), p.getRole(),
                stack, p.getStartDate(), p.getEndDate(),
                p.getGithubUrl(), p.getDeployUrl(), p.getStatus()
        );
    }
}
