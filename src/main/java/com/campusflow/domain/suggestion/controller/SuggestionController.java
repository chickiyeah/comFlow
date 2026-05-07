package com.campusflow.domain.suggestion.controller;

import com.campusflow.domain.suggestion.dto.SuggestionRequest;
import com.campusflow.domain.suggestion.dto.SuggestionResponse;
import com.campusflow.domain.suggestion.entity.SuggestionStatus;
import com.campusflow.domain.suggestion.service.SuggestionService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    // 누구나 익명 제출 가능
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SuggestionResponse> submit(@Valid @RequestBody SuggestionRequest request) {
        return ApiResponse.ok(suggestionService.submit(request));
    }

    // 관리자만 전체 조회
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<SuggestionResponse>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(suggestionService.getAll(pageable));
    }

    // 관리자 답변
    @PatchMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SuggestionResponse> reply(
            @PathVariable Long id,
            @RequestParam String reply,
            @RequestParam SuggestionStatus status) {
        return ApiResponse.ok(suggestionService.reply(id, reply, status));
    }
}
