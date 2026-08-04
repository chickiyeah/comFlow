package com.campusflow.domain.jobpilot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 자기소개서 문항 1개.
 * charLimit / charLimitType 은 공고에 명시된 경우만 채워지고, 없으면 null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EssayQuestion(
        String question,        // 문항 제목/내용
        Integer charLimit,      // 글자수 제한 (없으면 null)
        String charLimitType    // '공백포함' | '공백제외' | null
) {}
