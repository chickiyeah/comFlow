package com.campusflow.domain.award.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AwardLevel {
    GOLD("금상"),
    SILVER("은상"),
    BRONZE("동상"),
    ENCOURAGEMENT("장려상"),
    PARTICIPATION("입상");

    private final String label;
}
