package com.campusflow.domain.career.controller;

import com.campusflow.domain.career.dto.CareerActivityRequest;
import com.campusflow.domain.career.dto.CareerActivityResponse;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.service.CareerActivityService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/career/activities")
@RequiredArgsConstructor
public class CareerActivityController {

    private final CareerActivityService activityService;

    @GetMapping
    public ApiResponse<List<CareerActivityResponse>> getAll(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(activityService.getAll(username));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Long>> getSummary(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(activityService.getSummary(username));
    }

    @GetMapping("/type/{type}")
    public ApiResponse<List<CareerActivityResponse>> getByType(
            @AuthenticationPrincipal String username,
            @PathVariable ActivityType type) {
        return ApiResponse.ok(activityService.getByType(username, type));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CareerActivityResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CareerActivityRequest request) {
        return ApiResponse.ok(activityService.create(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CareerActivityResponse> update(
            @AuthenticationPrincipal String username,
            @PathVariable Long id,
            @Valid @RequestBody CareerActivityRequest request) {
        return ApiResponse.ok(activityService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        activityService.delete(username, id);
    }
}
