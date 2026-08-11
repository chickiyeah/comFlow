package com.campusflow.domain.assignment.controller;

import com.campusflow.domain.assignment.dto.GradebookResponse;
import com.campusflow.domain.assignment.service.GradebookService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping("/api/classes/{classId}/gradebook")
    public ApiResponse<GradebookResponse> gradebook(@AuthenticationPrincipal String username,
                                                    @PathVariable Long classId) {
        return ApiResponse.ok(gradebookService.gradebook(username, classId));
    }
}
