package com.campusflow.domain.interview.controller;

import com.campusflow.domain.interview.dto.InterviewResponse;
import com.campusflow.domain.interview.dto.StartInterviewRequest;
import com.campusflow.domain.interview.dto.SubmitAnswerRequest;
import com.campusflow.domain.interview.service.InterviewService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /** 세션 시작 (포트폴리오 분석 + 첫 질문 반환) */
    @PostMapping("/sessions")
    public ApiResponse<InterviewResponse.SessionStarted> startSession(
            @AuthenticationPrincipal String username,
            @RequestBody StartInterviewRequest req) {
        return ApiResponse.ok(interviewService.startSession(username, req));
    }

    /** 답변 제출 (피드백 + 다음 질문 or 종합 피드백) */
    @PostMapping("/sessions/{sessionId}/answer")
    public ApiResponse<InterviewResponse.AnswerResult> submitAnswer(
            @AuthenticationPrincipal String username,
            @PathVariable Long sessionId,
            @RequestBody SubmitAnswerRequest req) {
        return ApiResponse.ok(interviewService.submitAnswer(username, sessionId, req));
    }

    /** 내 면접 세션 목록 */
    @GetMapping("/sessions")
    public ApiResponse<List<InterviewResponse.SessionSummary>> listSessions(
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(interviewService.listSessions(username));
    }

    /** 세션 상세 (결과 조회) */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<InterviewResponse.SessionDetail> getSessionDetail(
            @AuthenticationPrincipal String username,
            @PathVariable Long sessionId) {
        return ApiResponse.ok(interviewService.getSessionDetail(username, sessionId));
    }
}
