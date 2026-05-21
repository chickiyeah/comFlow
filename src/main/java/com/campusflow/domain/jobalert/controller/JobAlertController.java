package com.campusflow.domain.jobalert.controller;

import com.campusflow.domain.jobalert.dto.JobAlertRequest;
import com.campusflow.domain.jobalert.dto.JobAlertResponse;
import com.campusflow.domain.jobalert.service.JobAlertService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career/alerts")
@RequiredArgsConstructor
public class JobAlertController {

    private final JobAlertService jobAlertService;

    @GetMapping
    public ApiResponse<List<JobAlertResponse>> getMyAlerts(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(jobAlertService.getMyAlerts(username));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobAlertResponse> create(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody JobAlertRequest request) {
        return ApiResponse.ok(jobAlertService.create(username, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long id) {
        jobAlertService.delete(username, id);
    }
}
