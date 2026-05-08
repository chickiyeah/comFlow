package com.campusflow.domain.user.controller;

import com.campusflow.domain.user.service.GitHubTokenService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/github-token")
@RequiredArgsConstructor
public class GitHubTokenController {

    private final GitHubTokenService tokenService;

    @GetMapping
    public ApiResponse<Map<String, Boolean>> getStatus(@AuthenticationPrincipal String username) {
        return ApiResponse.ok(Map.of("hasToken", tokenService.hasToken(username)));
    }

    @PostMapping
    public ApiResponse<Void> save(
            @AuthenticationPrincipal String username,
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ApiResponse.ok(null);
        }
        tokenService.saveToken(username, token.trim());
        return ApiResponse.ok(null);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String username) {
        tokenService.deleteToken(username);
    }
}
