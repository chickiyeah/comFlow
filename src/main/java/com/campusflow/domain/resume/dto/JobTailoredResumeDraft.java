package com.campusflow.domain.resume.dto;

import com.campusflow.domain.jobpilot.dto.MatchReport;

/** 공고 맞춤 이력서 초안 응답 — 저장 전 프론트로 반환. 매칭 리포트 + 대상 회사/직무 동봉. */
public record JobTailoredResumeDraft(ResumeDraft draft, MatchReport matchReport, String company, String position) {}
