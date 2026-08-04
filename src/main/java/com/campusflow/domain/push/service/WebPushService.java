package com.campusflow.domain.push.service;

import com.campusflow.domain.push.entity.PushSubscription;
import com.campusflow.domain.push.repository.PushSubscriptionRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Map;

/**
 * Web Push(VAPID) 발송 서비스. VAPID 키가 없으면 비활성(앱 기동에 영향 없음).
 * 인앱 알림 생성 시 NotificationService가 호출해 브라우저 푸시도 전송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    @Value("${vapid.public-key:}")  private String publicKey;
    @Value("${vapid.private-key:}") private String privateKey;
    @Value("${vapid.subject:mailto:noreply@campusflow.jvision.org}") private String subject;

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private PushService pushService;
    private boolean enabled = false;

    @PostConstruct
    void init() {
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            log.warn("[WebPush] VAPID 키 미설정 — 푸시 비활성");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(publicKey, privateKey, subject);
            enabled = true;
            log.info("[WebPush] VAPID 초기화 완료 — 푸시 활성");
        } catch (Exception e) {
            log.error("[WebPush] 초기화 실패 — 푸시 비활성: {}", e.getMessage());
        }
    }

    public String getPublicKey() {
        return enabled ? publicKey : "";
    }

    @Transactional
    public void subscribe(String username, String endpoint, String p256dh, String auth) {
        if (endpoint == null || endpoint.isBlank()) throw new BusinessException(ErrorCode.INVALID_INPUT);
        User user = getUser(username);
        // 같은 endpoint 재구독이면 중복 저장 방지
        if (subscriptionRepository.findByEndpoint(endpoint).isPresent()) return;
        subscriptionRepository.save(PushSubscription.builder()
                .user(user).endpoint(endpoint).p256dh(p256dh).auth(auth).build());
    }

    @Transactional
    public void unsubscribe(String username, String endpoint) {
        if (endpoint == null) return;
        subscriptionRepository.deleteByEndpoint(endpoint);
    }

    /** 사용자의 모든 구독 기기로 푸시 발송 (비동기, best-effort) */
    @Async
    public void sendToUser(User user, String title, String body, String url) {
        if (!enabled || user == null) return;
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title", title == null ? "CampusFlow" : title,
                    "body",  body == null ? "" : body,
                    "url",   url == null ? "/" : url));
        } catch (Exception e) { return; }

        for (PushSubscription sub : subscriptionRepository.findByUserId(user.getId())) {
            try {
                pushService.send(new Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                log.warn("[WebPush] 발송 실패 endpoint={}...: {}",
                        sub.getEndpoint().substring(0, Math.min(40, sub.getEndpoint().length())), e.getMessage());
            }
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
