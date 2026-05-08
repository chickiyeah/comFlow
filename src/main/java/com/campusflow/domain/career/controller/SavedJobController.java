package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.SavedJobRequest;
import com.campusflow.domain.career.dto.SavedJobResponse;
import com.campusflow.domain.career.service.SavedJobService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career/saved-jobs")
@RequiredArgsConstructor
public class SavedJobController {

    private final SavedJobService savedJobService;

    @GetMapping
    public ApiResponse<List<SavedJobResponse>> getMine(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(savedJobService.getMine(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SavedJobResponse> save(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody SavedJobRequest request) {
        return ApiResponse.ok(savedJobService.save(username, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        savedJobService.delete(username, id);
    }
}
