package com.campusflow.domain.user.controller;

import com.campusflow.domain.user.dto.EmailSendRequest;
import com.campusflow.domain.user.dto.EmailVerifyRequest;
import com.campusflow.domain.user.dto.LoginRequest;
import com.campusflow.domain.user.dto.RegisterRequest;
import com.campusflow.domain.user.dto.TokenResponse;
import com.campusflow.domain.user.service.AuthService;
import com.campusflow.domain.user.service.EmailVerificationService;
import com.campusflow.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
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
}
