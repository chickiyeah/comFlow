package com.campusflow.domain.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GithubGenerateRequest(
        @NotBlank(message = "GitHub URL을 입력해주세요.")
        @Pattern(regexp = "https://github\\.com/[^/]+/[^/]+.*",
                 message = "올바른 GitHub 저장소 URL을 입력해주세요.")
        String githubUrl
) {}
