package com.campusflow.domain.notice.controller;

import com.campusflow.domain.notice.dto.NoticeRequest;
import com.campusflow.domain.notice.dto.NoticeResponse;
import com.campusflow.domain.notice.service.NoticeService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<List<NoticeResponse>> getAll() {
        return ApiResponse.ok(noticeService.getAll());
    }

    @GetMapping("/recent")
    public ApiResponse<List<NoticeResponse>> getRecent(
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.ok(noticeService.getRecent(limit));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody NoticeRequest request) {
        return ApiResponse.ok(noticeService.create(username, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        noticeService.delete(username, id);
    }
}
