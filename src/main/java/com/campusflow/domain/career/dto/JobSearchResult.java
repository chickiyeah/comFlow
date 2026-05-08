package com.campusflow.domain.career.dto;

import java.time.LocalDate;

public record JobSearchResult(
        String id,
        String title,
        String company,
        String location,
        String url,
        LocalDate deadline,
        String jobType,
        String salary,
        String source
) {}
