package com.campusflow.domain.graduation.controller;

import com.campusflow.domain.graduation.dto.GraduationCheckResponse;
import com.campusflow.domain.graduation.service.GraduationService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graduation")
@RequiredArgsConstructor
public class GraduationController {

    private final GraduationService graduationService;

    @GetMapping("/check")
    public ApiResponse<GraduationCheckResponse> checkGraduation(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(graduationService.checkGraduation(userDetails.getUsername()));
    }
}
