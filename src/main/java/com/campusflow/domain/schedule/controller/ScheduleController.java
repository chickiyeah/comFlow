package com.campusflow.domain.schedule.controller;

import com.campusflow.domain.schedule.dto.ScheduleRequest;
import com.campusflow.domain.schedule.dto.ScheduleResponse;
import com.campusflow.domain.schedule.service.ScheduleService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/me/today")
    public ApiResponse<List<ScheduleResponse>> getToday(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(scheduleService.getToday(username));
    }

    @GetMapping("/me")
    public ApiResponse<List<ScheduleResponse>> getAll(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(scheduleService.getAll(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.create(username, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponse> update(
            @AuthenticationPrincipal String username,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.update(username, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        scheduleService.delete(username, id);
    }
}
