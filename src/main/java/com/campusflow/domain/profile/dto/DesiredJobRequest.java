package com.campusflow.domain.profile.dto;

import jakarta.validation.constraints.Size;

public record DesiredJobRequest(
        @Size(max = 100) String desiredJob
) {}
