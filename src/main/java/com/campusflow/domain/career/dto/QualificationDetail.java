package com.campusflow.domain.career.dto;

public record QualificationDetail(
        String jmNm,            // 종목명
        String qualgbNm,        // 자격구분
        String engJmNm,         // 영문 종목명
        String relatedDept,     // 관련 학과
        String applyQual,       // 응시자격
        String examMethod,      // 검정방법
        String passStandard,    // 합격기준
        String feeWritten,      // 필기 수수료
        String feePractical     // 실기 수수료
) {}
