package com.campusflow.domain.quiz.controller;

import com.campusflow.domain.quiz.dto.*;
import com.campusflow.domain.quiz.service.QuizService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** 퀴즈 목록 (courseId로 필터 가능) */
    @GetMapping
    public ApiResponse<List<QuizResponse>> list(@RequestParam(required = false) Long courseId) {
        return ApiResponse.ok(quizService.list(courseId));
    }

    /** 응시용 상세 (정답 제외) */
    @GetMapping("/{id}")
    public ApiResponse<QuizDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(quizService.getForTaking(id));
    }

    /** 퀴즈 생성 (교직원) */
    @PostMapping
    public ApiResponse<QuizResponse> create(@AuthenticationPrincipal String username,
                                            @Valid @RequestBody QuizRequest request) {
        return ApiResponse.ok(quizService.create(username, request));
    }

    /** AI 자동 출제 — 초안 반환(저장 X). 교직원이 검토 후 저장 */
    @PostMapping("/generate")
    public ApiResponse<QuizRequest> generate(@AuthenticationPrincipal String username,
                                             @Valid @RequestBody QuizGenerateRequest request) {
        return ApiResponse.ok(quizService.generateDraft(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuizResponse> update(@AuthenticationPrincipal String username, @PathVariable Long id,
                                            @Valid @RequestBody QuizRequest request) {
        return ApiResponse.ok(quizService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        quizService.delete(username, id);
        return ApiResponse.ok(null);
    }

    /** 학생 응시 제출 → 자동/AI 채점 결과 */
    @PostMapping("/{id}/submit")
    public ApiResponse<QuizResultResponse> submit(@AuthenticationPrincipal String username, @PathVariable Long id,
                                                  @RequestBody QuizSubmitRequest request) {
        return ApiResponse.ok(quizService.submit(username, id, request));
    }

    /** 내 결과 */
    @GetMapping("/{id}/my-result")
    public ApiResponse<QuizResultResponse> myResult(@AuthenticationPrincipal String username, @PathVariable Long id) {
        return ApiResponse.ok(quizService.myResult(username, id));
    }

    /** 제출 목록 (교직원) */
    @GetMapping("/{id}/submissions")
    public ApiResponse<List<StaffSubmissionResponse>> submissions(@AuthenticationPrincipal String username,
                                                                  @PathVariable Long id) {
        return ApiResponse.ok(quizService.submissions(username, id));
    }

    /** 답안 채점 검토(교수 override). body: { score, feedback } */
    @PutMapping("/answers/{answerId}")
    public ApiResponse<Void> override(@AuthenticationPrincipal String username, @PathVariable Long answerId,
                                      @RequestBody Map<String, Object> body) {
        int score = body.get("score") instanceof Number n ? n.intValue() : 0;
        String feedback = body.get("feedback") != null ? body.get("feedback").toString() : "";
        quizService.overrideAnswer(username, answerId, score, feedback);
        return ApiResponse.ok(null);
    }
}
