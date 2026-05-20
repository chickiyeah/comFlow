package com.campusflow.domain.career.dto;

public record PracticeQuestion(
        String jmNm,        // 종목명
        String implYy,      // 시행년도
        String implSeq,     // 회차
        String fileUrl,     // 파일 다운로드 URL
        String qualgbNm     // 자격구분
) {}
