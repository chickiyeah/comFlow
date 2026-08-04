package com.campusflow.domain.user.controller;

import com.campusflow.domain.user.dto.EmailSendRequest;
import com.campusflow.domain.user.dto.EmailVerifyRequest;
import com.campusflow.domain.user.dto.FindUsernameRequest;
import com.campusflow.domain.user.dto.FindUsernameResponse;
import com.campusflow.domain.user.dto.LoginRequest;
import com.campusflow.domain.user.dto.RegisterRequest;
import com.campusflow.domain.user.dto.ResetPasswordRequest;
import com.campusflow.domain.user.dto.TokenResponse;
import com.campusflow.domain.session.service.UserSessionService;
import com.campusflow.domain.user.service.AuthService;
import com.campusflow.domain.user.service.EmailVerificationService;
import com.campusflow.global.response.ApiResponse;
import com.campusflow.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final UserSessionService sessionService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request,
                httpRequest.getHeader("User-Agent"), clientIp(httpRequest)));
    }

    /** 로그아웃 — 현재 기기 세션 폐기 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        String bearer = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            try { sessionService.revokeByJti(tokenProvider.getJti(bearer.substring(7))); }
            catch (Exception ignored) { /* 이미 무효한 토큰이면 무시 */ }
        }
        return ApiResponse.ok(null);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmailCode(@Valid @RequestBody EmailSendRequest request) {
        emailVerificationService.sendCode(request.email());
        return ApiResponse.ok("인증 코드가 발송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ApiResponse<Void> verifyEmailCode(@Valid @RequestBody EmailVerifyRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.ok("이메일 인증이 완료되었습니다.");
    }

    @PostMapping("/find-username")
    public ApiResponse<FindUsernameResponse> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        return ApiResponse.ok(new FindUsernameResponse(authService.findUsername(request.email())));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.code(), request.newPassword());
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }
}
