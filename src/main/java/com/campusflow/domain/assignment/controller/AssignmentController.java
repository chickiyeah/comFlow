package com.campusflow.domain.assignment.controller;

import com.campusflow.domain.assignment.dto.*;
import com.campusflow.domain.assignment.service.AssignmentCommentService;
import com.campusflow.domain.assignment.service.AssignmentService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentCommentService commentService;

    // ── 목록/생성 (클래스 스코프) ────────────────────────────
    @GetMapping("/api/classes/{classId}/assignments")
    public ApiResponse<List<AssignmentResponse>> list(@AuthenticationPrincipal String username,
                                                      @PathVariable Long classId) {
        return ApiResponse.ok(assignmentService.list(username, classId));
    }

    @PostMapping("/api/classes/{classId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> create(@AuthenticationPrincipal String username,
                                                  @PathVariable Long classId,
                                                  @Valid @RequestBody AssignmentCreateRequest request) {
        return ApiResponse.ok(assignmentService.create(username, classId, request));
    }

    // ── 개별 과제 ────────────────────────────────────────────
    @GetMapping("/api/assignments/{assignmentId}")
    public ApiResponse<AssignmentDetailResponse> detail(@AuthenticationPrincipal String username,
                                                        @PathVariable Long assignmentId) {
        return ApiResponse.ok(assignmentService.getDetail(username, assignmentId));
    }

    @PutMapping("/api/assignments/{assignmentId}")
    public ApiResponse<AssignmentResponse> update(@AuthenticationPrincipal String username,
                                                  @PathVariable Long assignmentId,
                                                  @Valid @RequestBody AssignmentUpdateRequest request) {
        return ApiResponse.ok(assignmentService.update(username, assignmentId, request));
    }

    @PatchMapping("/api/assignments/{assignmentId}/draft")
    public ApiResponse<AssignmentResponse> draft(@AuthenticationPrincipal String username,
                                                 @PathVariable Long assignmentId,
                                                 @Valid @RequestBody DraftRequest request) {
        return ApiResponse.ok(assignmentService.updateDraft(username, assignmentId, request.draft()));
    }

    @PatchMapping("/api/assignments/{assignmentId}/topic")
    public ApiResponse<AssignmentResponse> topic(@AuthenticationPrincipal String username,
                                                 @PathVariable Long assignmentId,
                                                 @Valid @RequestBody TopicRequest request) {
        return ApiResponse.ok(assignmentService.updateTopic(username, assignmentId, request.topic()));
    }

    @DeleteMapping("/api/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long assignmentId) {
        assignmentService.delete(username, assignmentId);
    }

    @PostMapping("/api/assignments/{assignmentId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentFileResponse> attachFile(@AuthenticationPrincipal String username,
                                                          @PathVariable Long assignmentId,
                                                          @RequestParam MultipartFile file) {
        return ApiResponse.ok(assignmentService.attachFile(username, assignmentId, file));
    }

    // ── 제출 ─────────────────────────────────────────────────
    @PostMapping("/api/assignments/{assignmentId}/submit")
    public ApiResponse<SubmissionResponse> submit(@AuthenticationPrincipal String username,
                                                  @PathVariable Long assignmentId,
                                                  @RequestParam(required = false) String content,
                                                  @RequestParam(required = false) MultipartFile file) {
        return ApiResponse.ok(assignmentService.submit(username, assignmentId, content, file));
    }

    @GetMapping("/api/assignments/{assignmentId}/submissions")
    public ApiResponse<List<SubmissionResponse>> submissions(@AuthenticationPrincipal String username,
                                                             @PathVariable Long assignmentId) {
        return ApiResponse.ok(assignmentService.submissions(username, assignmentId));
    }

    @GetMapping("/api/assignments/{assignmentId}/submission-stats")
    public ApiResponse<SubmissionStatsResponse> stats(@AuthenticationPrincipal String username,
                                                      @PathVariable Long assignmentId) {
        return ApiResponse.ok(assignmentService.stats(username, assignmentId));
    }

    // ── 비공개 코멘트 ────────────────────────────────────────
    @GetMapping("/api/assignments/{assignmentId}/comments")
    public ApiResponse<List<CommentResponse>> comments(@AuthenticationPrincipal String username,
                                                       @PathVariable Long assignmentId,
                                                       @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(commentService.thread(username, assignmentId, studentId));
    }

    @PostMapping("/api/assignments/{assignmentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> addComment(@AuthenticationPrincipal String username,
                                                   @PathVariable Long assignmentId,
                                                   @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(commentService.add(username, assignmentId, request));
    }
}
