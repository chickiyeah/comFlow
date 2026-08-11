package com.campusflow.domain.course.controller;

import com.campusflow.domain.course.dto.CourseCommentRequest;
import com.campusflow.domain.course.dto.CourseCommentResponse;
import com.campusflow.domain.course.dto.CourseRequest;
import com.campusflow.domain.course.dto.CourseResponse;
import com.campusflow.domain.course.service.CourseCommentService;
import com.campusflow.domain.course.service.CourseService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseCommentService commentService;

    /** 활성 강좌 목록 (학생·교직원 공통) */
    @GetMapping
    public ApiResponse<List<CourseResponse>> list() {
        return ApiResponse.ok(courseService.listActive());
    }

    /** 교직원 관리용 — 비활성 포함 전체 */
    @GetMapping("/manage")
    public ApiResponse<List<CourseResponse>> manage(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(courseService.listAll(username));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(courseService.get(id));
    }

    /** 강좌 등록 (교직원) — 전체 학생에게 알림 broadcast */
    @PostMapping
    public ApiResponse<CourseResponse> create(@AuthenticationPrincipal String username,
                                              @Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(courseService.create(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> update(@AuthenticationPrincipal String username,
                                              @PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(courseService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        courseService.delete(username, id);
        return ApiResponse.ok(null);
    }

    /** 학생 시청 기록 */
    @PostMapping("/{id}/view")
    public ApiResponse<Void> recordView(@AuthenticationPrincipal String username, @PathVariable Long id) {
        courseService.recordView(username, id);
        return ApiResponse.ok(null);
    }

    /** 수강 완료 — 취업활동(교육/연수)에 자동 기록 */
    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        courseService.complete(username, id);
        return ApiResponse.ok(null);
    }

    /** 진도 저장 (이어보기·90% 자동완료). body: { positionSec, durationSec } */
    @PostMapping("/{id}/progress")
    public ApiResponse<Void> saveProgress(@AuthenticationPrincipal String username, @PathVariable Long id,
                                          @RequestBody java.util.Map<String, Integer> body) {
        courseService.saveProgress(username, id,
                body.getOrDefault("positionSec", 0), body.getOrDefault("durationSec", 0));
        return ApiResponse.ok(null);
    }

    /** 수료증 PDF 다운로드 (수강 완료한 강좌) */
    @GetMapping("/{id}/certificate")
    public org.springframework.http.ResponseEntity<byte[]> certificate(
            @AuthenticationPrincipal String username, @PathVariable Long id) {
        byte[] pdf = courseService.generateCertificate(username, id);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificate-" + id + ".pdf\"")
                .body(pdf);
    }

    /** 내 강좌 진도 목록 */
    @GetMapping("/my-progress")
    public ApiResponse<List<com.campusflow.domain.course.dto.CourseProgressResponse>> myProgress(
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(courseService.myProgress(username));
    }

    // ── 강좌 Q&A (댓글/질문 + AI 조교) ──────────────────────────
    @GetMapping("/{id}/comments")
    public ApiResponse<List<CourseCommentResponse>> comments(@PathVariable Long id) {
        return ApiResponse.ok(commentService.list(id));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<List<CourseCommentResponse>> addComment(@AuthenticationPrincipal String username,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody CourseCommentRequest request) {
        return ApiResponse.ok(commentService.add(username, id, request));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal String username, @PathVariable Long commentId) {
        commentService.delete(username, commentId);
        return ApiResponse.ok(null);
    }
}

