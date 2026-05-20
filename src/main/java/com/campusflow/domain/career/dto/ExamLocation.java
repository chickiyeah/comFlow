package com.campusflow.domain.career.dto;

public record ExamLocation(
        String brchNm,      // 지사명
        String placNm,      // 시험장명
        String addr,        // 주소
        String telNo        // 전화번호
) {}
