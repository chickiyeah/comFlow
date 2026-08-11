package com.campusflow.domain.resume.dto;

import java.util.List;

/** 정직성 자동수정 로그 — 무엇이 과장으로 감지되어 어떻게 고쳐졌는지 사용자에게 투명 노출. */
public record HonestyReport(List<Fix> fixes) {
    public record Fix(String section, String before, String after, String reason) {}
}
