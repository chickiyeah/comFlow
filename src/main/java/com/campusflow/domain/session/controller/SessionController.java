package com.campusflow.domain.session.controller;

import com.campusflow.domain.session.dto.SessionResponse;
import com.campusflow.domain.session.service.UserSessionService;
import com.campusflow.global.response.ApiResponse;
import com.campusflow.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserSessionService sessionService;

    /** 내 접속 기기(세션) 목록 — 현재 기기는 current=true */
    @GetMapping
    public ApiResponse<List<SessionResponse>> list(@AuthenticationPrincipal String username,
                                                   HttpServletRequest request) {
        return ApiResponse.ok(sessionService.list(username, currentJti(request)));
    }

    /** 특정 기기 원격 로그아웃 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(@AuthenticationPrincipal String username, @PathVariable Long id) {
        sessionService.revoke(username, id);
        return ApiResponse.ok(null);
    }

    /** 현재 기기를 제외한 다른 모든 기기 로그아웃 */
    @DeleteMapping("/others")
    public ApiResponse<Map<String, Integer>> revokeOthers(@AuthenticationPrincipal String username,
                                                          HttpServletRequest request) {
        int revoked = sessionService.revokeOthers(username, currentJti(request));
        return ApiResponse.ok(Map.of("revoked", revoked));
    }

    private String currentJti(HttpServletRequest request) {
        Object jti = request.getAttribute(JwtAuthenticationFilter.JTI_ATTR);
        return jti != null ? jti.toString() : null;
    }
}
