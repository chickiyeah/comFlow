package com.campusflow.domain.classattendance.controller;

import com.campusflow.domain.classattendance.dto.*;
import com.campusflow.domain.classattendance.service.ClassAttendanceService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클래스별 출석. 목록/생성/내출결은 클래스 스코프, 세션 단위 작업은 /api/class-attendance/{sessionId}
 * (기존 학과 attendance 도메인 /api/attendance 와 분리).
 */
@RestController
@RequiredArgsConstructor
public class ClassAttendanceController {

    private final ClassAttendanceService attendanceService;

    @GetMapping("/api/classes/{classId}/attendance")
    public ApiResponse<List<SessionResponse>> list(@AuthenticationPrincipal String username,
                                                   @PathVariable Long classId) {
        return ApiResponse.ok(attendanceService.listSessions(username, classId));
    }

    @PostMapping("/api/classes/{classId}/attendance")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionResponse> create(@AuthenticationPrincipal String username,
                                               @PathVariable Long classId,
                                               @Valid @RequestBody SessionCreateRequest request) {
        return ApiResponse.ok(attendanceService.createSession(username, classId, request));
    }

    @GetMapping("/api/classes/{classId}/attendance/me")
    public ApiResponse<List<MyAttendanceResponse>> myAttendance(@AuthenticationPrincipal String username,
                                                                @PathVariable Long classId) {
        return ApiResponse.ok(attendanceService.myAttendance(username, classId));
    }

    @GetMapping("/api/class-attendance/{sessionId}")
    public ApiResponse<SessionResponse> session(@AuthenticationPrincipal String username,
                                                @PathVariable Long sessionId) {
        return ApiResponse.ok(attendanceService.getSession(username, sessionId));
    }

    @PatchMapping("/api/class-attendance/{sessionId}/mark")
    public ApiResponse<SessionResponse> mark(@AuthenticationPrincipal String username,
                                             @PathVariable Long sessionId,
                                             @Valid @RequestBody MarkRequest request) {
        return ApiResponse.ok(attendanceService.mark(username, sessionId, request));
    }

    @DeleteMapping("/api/class-attendance/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username, @PathVariable Long sessionId) {
        attendanceService.deleteSession(username, sessionId);
    }
}
