package com.campusflow.domain.graduation.dto;

import java.util.List;

public record GraduationCheckResponse(
        boolean isEligible,
        double currentGpa,
        int totalEarnedCredits,
        int requiredTotalCredits,
        List<RequirementStatus> requirements
) {
    public record RequirementStatus(
            String category,
            String name,
            int earnedCredits,
            int requiredCredits,
            boolean completed
    ) {}
}
