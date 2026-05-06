package com.campusflow.domain.attendance.controller;

import com.campusflow.domain.attendance.dto.AttendanceSummaryResponse;
import com.campusflow.domain.attendance.service.AttendanceService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/me")
    public ApiResponse<AttendanceSummaryResponse> getMySummary(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.ok(attendanceService.getMySummary(userDetails.getUsername()));
    }
}
