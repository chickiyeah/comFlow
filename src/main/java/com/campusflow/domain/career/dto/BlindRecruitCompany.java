package com.campusflow.domain.career.dto;

public record BlindRecruitCompany(
        String insttNm,     // 기관명
        String ncsDivNm,    // NCS 대분류명
        String recrutField, // 채용 분야
        String year         // 연도
) {}
