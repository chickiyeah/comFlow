package com.campusflow.domain.resume.dto;

/** 생성 초안 — 저장 전 프론트로 반환. honestyReport로 자동수정 내역 투명 노출. */
public record ResumeDraft(ResumeData data, HonestyReport honestyReport, String template) {}
