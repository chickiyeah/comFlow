package com.campusflow.domain.jobalert.dto;

import jakarta.validation.constraints.NotBlank;

public record JobAlertRequest(
        @NotBlank String keyword,
        String region,
        String empType
) {}
