package com.campusflow.domain.coverletter.controller;

import com.campusflow.domain.coverletter.dto.CoverLetterResponse;
import com.campusflow.domain.coverletter.dto.CoverLetterSaveRequest;
import com.campusflow.domain.coverletter.service.CoverLetterService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cover-letters")
@RequiredArgsConstructor
public class CoverLetterController {

    private final CoverLetterService coverLetterService;

    @GetMapping
    public ApiResponse<List<CoverLetterResponse>> getMine(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(coverLetterService.getMine(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CoverLetterResponse> save(@AuthenticationPrincipal String username,
                                                  @Valid @RequestBody CoverLetterSaveRequest request) {
        return ApiResponse.ok(coverLetterService.save(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CoverLetterResponse> update(@AuthenticationPrincipal String username,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody CoverLetterSaveRequest request) {
        return ApiResponse.ok(coverLetterService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        coverLetterService.delete(username, id);
    }
}
