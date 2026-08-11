package com.campusflow.domain.assignment.controller;

import com.campusflow.domain.assignment.dto.AiCheckResponse;
import com.campusflow.domain.assignment.dto.GradeRequest;
import com.campusflow.domain.assignment.dto.SubmissionResponse;
import com.campusflow.domain.assignment.service.AssignmentAiService;
import com.campusflow.domain.assignment.service.AssignmentService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final AssignmentService assignmentService;
    private final AssignmentAiService assignmentAiService;

    @PostMapping("/{submissionId}/grade")
    public ApiResponse<SubmissionResponse> grade(@AuthenticationPrincipal String username,
                                                 @PathVariable Long submissionId,
                                                 @Valid @RequestBody GradeRequest request) {
        return ApiResponse.ok(assignmentService.grade(username, submissionId, request));
    }

    @PostMapping("/{submissionId}/return")
    public ApiResponse<SubmissionResponse> returnSubmission(@AuthenticationPrincipal String username,
                                                            @PathVariable Long submissionId) {
        return ApiResponse.ok(assignmentService.returnSubmission(username, submissionId));
    }

    @PostMapping("/{submissionId}/ai-check")
    public ApiResponse<AiCheckResponse> aiCheck(@AuthenticationPrincipal String username,
                                                @PathVariable Long submissionId) {
        return ApiResponse.ok(assignmentAiService.check(username, submissionId));
    }
}
