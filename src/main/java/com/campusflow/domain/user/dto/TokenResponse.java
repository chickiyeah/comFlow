package com.campusflow.domain.user.dto;

public record TokenResponse(String accessToken, String tokenType, String name, String role) {
    public TokenResponse(String accessToken, String name, String role) {
        this(accessToken, "Bearer", name, role);
    }
}
