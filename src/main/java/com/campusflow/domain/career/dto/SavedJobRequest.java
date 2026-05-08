package com.campusflow.domain.career.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SavedJobRequest(
        @NotBlank String title,
        @NotBlank String company,
        String location,
        String url,
        LocalDate deadline,
        String jobType,
        String salary,
        String description,
        String source
) {}
