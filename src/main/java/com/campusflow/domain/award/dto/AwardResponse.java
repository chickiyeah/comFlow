package com.campusflow.domain.award.dto;

import com.campusflow.domain.award.entity.Award;
import com.campusflow.domain.award.entity.AwardLevel;

import java.time.LocalDate;

public record AwardResponse(
        Long id,
        String title,
        String organization,
        AwardLevel level,
        String levelLabel,
        LocalDate awardDate,
        String description
) {
    public static AwardResponse from(Award a) {
        return new AwardResponse(
                a.getId(), a.getTitle(), a.getOrganization(),
                a.getLevel(), a.getLevel().getLabel(),
                a.getAwardDate(), a.getDescription()
        );
    }
}
