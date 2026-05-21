package com.campusflow.domain.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record SyncRequest(@NotBlank String schoolPassword) {}
