package com.campusflow.domain.coverletter.dto;

import jakarta.validation.constraints.NotBlank;

public record CoverLetterSaveRequest(
        @NotBlank(message = "제목을 입력해주세요.") String title,
        @NotBlank(message = "회사명을 입력해주세요.") String companyName,
        @NotBlank(message = "직무를 입력해주세요.") String jobTitle,
        @NotBlank(message = "내용을 입력해주세요.") String content
) {}
