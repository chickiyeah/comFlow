package com.campusflow.domain.jobpilot.controller;

import com.campusflow.domain.jobpilot.dto.GenerateReport;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import com.campusflow.domain.jobpilot.dto.JobPilotRequests.*;
import com.campusflow.domain.jobpilot.service.JdExtractorService;
import com.campusflow.domain.jobpilot.service.JobCollectorService;
import com.campusflow.domain.jobpilot.service.JobPilotService;
import com.campusflow.domain.jobpilot.service.ProfileAssembler;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * JobPilot — 채용 공고 → 직무 맞춤 자소서 파이프라인.
 *   [수집] collect → [추출] extract → [매칭] match → [생성] generate
 * 검토 UI 는 단계별 결과를 받아 사용자가 수정·내보낸다 (자동 제출 금지, 휴먼 검토 필수).
 */
@RestController
@RequestMapping("/api/jobpilot")
@RequiredArgsConstructor
public class JobPilotController {

    private final JobCollectorService collectorService;
    private final JdExtractorService extractorService;
    private final JobPilotService jobPilotService;
    private final ProfileAssembler profileAssembler;

    /** 로그인 학생의 프로필 근거(자동 조립). 검토 UI 에서 보정용으로 표시. */
    @GetMapping("/profile")
    public ApiResponse<StudentProfileDto> profile(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(profileAssembler.assemble(username));
    }

    /** [수집] URL 1건 on-demand fetch 또는 붙여넣기 → 공고 원문. */
    @PostMapping("/collect")
    public ApiResponse<CollectResult> collect(@AuthenticationPrincipal String username,
                                              @RequestBody CollectRequest request) {
        return ApiResponse.ok(collectorService.collect(username, request.url(), request.text()));
    }

    /** [추출] 공고 원문 → 구조화 JobPosting. */
    @PostMapping("/extract")
    public ApiResponse<JobPosting> extract(@AuthenticationPrincipal String username,
                                           @Valid @RequestBody ExtractRequest request) {
        return ApiResponse.ok(extractorService.extract(request.text()));
    }

    /** [매칭] JobPosting + 프로필 → 강점/약점 리포트. */
    @PostMapping("/match")
    public ApiResponse<MatchReport> match(@AuthenticationPrincipal String username,
                                          @RequestBody MatchRequest request) {
        return ApiResponse.ok(jobPilotService.match(username, request));
    }

    /** [생성] JobPosting(+매칭) + 프로필 → 문항별 자소서 + 글자수 리포트. */
    @PostMapping("/generate")
    public ApiResponse<GenerateReport> generate(@AuthenticationPrincipal String username,
                                                @RequestBody GenerateRequest request) {
        return ApiResponse.ok(jobPilotService.generate(username, request));
    }
}
