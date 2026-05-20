package com.campusflow.domain.career.dto;

public record QualificationItem(
        String seriesCd,    // 계열코드
        String jmCd,        // 종목코드
        String jmNm,        // 종목명
        String qualgbCd,    // 자격구분코드 (T=기술, C=전문, W=기능사보)
        String qualgbNm,    // 자격구분명
        String instiNm      // 시행기관명
) {}
