package com.campusflow.domain.review.controller;

import com.campusflow.domain.review.dto.CourseReviewRequest;
import com.campusflow.domain.review.dto.CourseReviewResponse;
import com.campusflow.domain.review.service.CourseReviewService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService reviewService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getBySubject(
            @RequestParam String subject,
            @AuthenticationPrincipal String username) {
        return ApiResponse.ok(reviewService.getBySubject(subject, username));
    }

    @GetMapping("/me")
    public ApiResponse<List<CourseReviewResponse>> getMyReviews(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(reviewService.getMyReviews(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseReviewResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CourseReviewRequest request) {
        return ApiResponse.ok(reviewService.create(username, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        reviewService.delete(username, id);
    }
}
