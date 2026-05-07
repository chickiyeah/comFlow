package com.campusflow.domain.suggestion.dto;

import com.campusflow.domain.suggestion.entity.SuggestionCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuggestionRequest(
        @NotNull(message = "카테고리를 선택해주세요.") SuggestionCategory category,
        @NotBlank(message = "내용을 입력해주세요.")
        @Size(min = 10, max = 1000, message = "10자 이상 1000자 이하로 작성해주세요.")
        String content
) {}
