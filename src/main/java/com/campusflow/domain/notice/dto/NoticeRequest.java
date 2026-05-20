package com.campusflow.domain.notice.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeRequest(
        @NotBlank String title,
        String summary,
        String content,
        boolean important
) {}
