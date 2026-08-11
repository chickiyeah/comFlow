package com.campusflow.domain.jobpilot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JobPilot 컨트롤러 요청/응답 DTO 묶음.
 */
public final class JobPilotRequests {

    private JobPilotRequests() {}

    /** [수집] 요청. url 또는 text 중 하나는 있어야 한다 (서비스에서 검증). */
    public record CollectRequest(String url, String text) {}

    /** [수집] 응답. */
    public record CollectResult(
            String source,      // 'url' | 'paste'
            String url,
            String rawText,
            String fetchedAt,   // ISO-8601
            boolean cached      // 캐시 히트 여부
    ) {}

    /** [추출] 요청. */
    public record ExtractRequest(
            @NotBlank(message = "공고 텍스트를 입력해주세요.") String text,
            String url          // 선택 — 추출 결과 캐싱/저장용 출처
    ) {}

    /** [매칭] 요청. profile 생략 시 로그인 학생 프로필 자동 조립. */
    public record MatchRequest(
            JobPosting job,
            StudentProfileDto profile
    ) {}

    /** [생성] 요청. matchReport·profile 생략 시 서버가 채운다. */
    public record GenerateRequest(
            JobPosting job,
            MatchReport matchReport,
            StudentProfileDto profile
    ) {}
}
