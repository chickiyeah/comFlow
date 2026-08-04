package com.campusflow.domain.push.controller;

import com.campusflow.domain.push.service.WebPushService;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final WebPushService webPushService;

    /** 프론트가 PushManager.subscribe에 쓸 VAPID 공개키 (비어있으면 푸시 비활성) */
    @GetMapping("/public-key")
    public ApiResponse<Map<String, String>> publicKey() {
        return ApiResponse.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    /** 브라우저 PushSubscription 등록. body: { endpoint, keys: { p256dh, auth } } */
    @PostMapping("/subscribe")
    @SuppressWarnings("unchecked")
    public ApiResponse<Void> subscribe(@AuthenticationPrincipal String username,
                                       @RequestBody Map<String, Object> body) {
        String endpoint = (String) body.get("endpoint");
        Map<String, Object> keys = (Map<String, Object>) body.get("keys");
        String p256dh = keys != null ? (String) keys.get("p256dh") : null;
        String auth   = keys != null ? (String) keys.get("auth")   : null;
        webPushService.subscribe(username, endpoint, p256dh, auth);
        return ApiResponse.ok(null);
    }

    /** 구독 해제. body: { endpoint } */
    @PostMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribe(@AuthenticationPrincipal String username,
                                         @RequestBody Map<String, String> body) {
        webPushService.unsubscribe(username, body.get("endpoint"));
        return ApiResponse.ok(null);
    }
}
