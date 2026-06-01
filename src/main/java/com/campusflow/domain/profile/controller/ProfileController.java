package com.campusflow.domain.profile.controller;

import com.campusflow.domain.profile.dto.AcademicUpdateRequest;
import com.campusflow.domain.profile.dto.ProfileResponse;
import com.campusflow.domain.profile.dto.SyncRequest;
import com.campusflow.domain.profile.service.ProfileSyncService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileSyncService profileSyncService;

    /** 현재 프로필 조회 */
    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(profileSyncService.getProfile(username));
    }

    /**
     * 학교 포털 연동 활성화 + 즉시 동기화
     * body: { schoolPassword: "비밀번호" }
     * 비밀번호는 서버에 저장되지 않음
     */
    @PostMapping("/sync")
    public ApiResponse<ProfileResponse> syncProfile(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody SyncRequest request) {
        return ApiResponse.ok(profileSyncService.syncWithPortal(username, request.schoolPassword(), request.studentId()));
    }

    /** 학년/학기 수정 */
    @PutMapping("/academic")
    public ApiResponse<ProfileResponse> updateAcademic(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody AcademicUpdateRequest request) {
        return ApiResponse.ok(profileSyncService.updateAcademic(username, request.grade(), request.semester()));
    }

    /** 연동 비활성화 */
    @DeleteMapping("/sync")
    public ApiResponse<ProfileResponse> disableSync(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(profileSyncService.disableSync(username));
    }

    /**
     * 학교 포털 데이터 조회 (저장된 세션 쿠키 + 토큰 사용)
     * sql_id 예시: hs/gr/gr01:gr_hakjum_list_s00
     */
    @GetMapping("/portal-data")
    public ApiResponse<Object> getPortalData(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String sqlId) {
        Object raw = profileSyncService.fetchPortalData(username, sqlId);
        return ApiResponse.ok(raw);
    }
}
