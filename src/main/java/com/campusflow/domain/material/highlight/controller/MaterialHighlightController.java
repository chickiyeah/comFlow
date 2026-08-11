package com.campusflow.domain.material.highlight.controller;

import com.campusflow.domain.material.highlight.dto.HighlightResponse;
import com.campusflow.domain.material.highlight.dto.HighlightStartRequest;
import com.campusflow.domain.material.highlight.service.MaterialHighlightService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials/{materialId}/highlights")
@RequiredArgsConstructor
public class MaterialHighlightController {

    private final MaterialHighlightService highlightService;

    @GetMapping
    public ApiResponse<HighlightResponse> get(@AuthenticationPrincipal String username,
                                              @PathVariable Long materialId) {
        return ApiResponse.ok(highlightService.get(username, materialId));
    }

    @PostMapping("/start")
    public ApiResponse<HighlightResponse> start(@AuthenticationPrincipal String username,
                                                @PathVariable Long materialId,
                                                @Valid @RequestBody HighlightStartRequest request) {
        return ApiResponse.ok(highlightService.start(username, materialId, request));
    }

    @PostMapping("/retry")
    public ApiResponse<HighlightResponse> retry(@AuthenticationPrincipal String username,
                                                @PathVariable Long materialId) {
        return ApiResponse.ok(highlightService.retry(username, materialId));
    }
}
