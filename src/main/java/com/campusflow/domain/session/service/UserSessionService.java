package com.campusflow.domain.session.service;

import com.campusflow.domain.session.dto.SessionResponse;
import com.campusflow.domain.session.entity.UserSession;
import com.campusflow.domain.session.repository.UserSessionRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;

    /** 세션 생성 결과 — jti와 동시접속(다른 활성 세션 존재) 여부 */
    public record Created(String jti, boolean concurrent) {}

    /** 로그인 시 세션 생성. 다른 활성 세션이 이미 있으면 concurrent=true. */
    @Transactional
    public Created createSession(User user, String userAgent, String ip) {
        boolean concurrent = sessionRepository.countByUserIdAndActiveTrue(user.getId()) > 0;
        String jti = UUID.randomUUID().toString().replace("-", "");
        sessionRepository.save(UserSession.builder()
                .user(user)
                .jti(jti)
                .device(parseDevice(userAgent))
                .userAgent(userAgent != null && userAgent.length() > 300 ? userAgent.substring(0, 300) : userAgent)
                .ip(ip)
                .build());
        return new Created(jti, concurrent);
    }

    /** 필터에서 매 요청 호출 — 세션이 살아있으면 true, lastSeen은 5분 이상 경과 시에만 갱신(쓰기 최소화). */
    @Transactional
    public boolean validateAndTouch(String jti) {
        if (jti == null) return false;
        UserSession s = sessionRepository.findByJti(jti).orElse(null);
        if (s == null || !s.isActive()) return false;
        if (Duration.between(s.getLastSeenAt(), LocalDateTime.now()).toMinutes() >= 5) {
            s.touch();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> list(String username, String currentJti) {
        Long uid = getUser(username).getId();
        return sessionRepository.findByUserIdAndActiveTrueOrderByLastSeenAtDesc(uid)
                .stream().map(s -> SessionResponse.from(s, currentJti)).toList();
    }

    @Transactional
    public void revoke(String username, Long sessionId) {
        Long uid = getUser(username).getId();
        UserSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!s.getUser().getId().equals(uid)) throw new BusinessException(ErrorCode.FORBIDDEN);
        s.revoke();
    }

    /** 현재 기기를 제외한 다른 모든 세션 폐기 */
    @Transactional
    public int revokeOthers(String username, String currentJti) {
        Long uid = getUser(username).getId();
        int count = 0;
        for (UserSession s : sessionRepository.findByUserIdAndActiveTrueOrderByLastSeenAtDesc(uid)) {
            if (!s.getJti().equals(currentJti)) { s.revoke(); count++; }
        }
        return count;
    }

    /** 로그아웃 — jti로 현재 세션 폐기 */
    @Transactional
    public void revokeByJti(String jti) {
        if (jti == null) return;
        sessionRepository.findByJti(jti).ifPresent(UserSession::revoke);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /** User-Agent에서 "브라우저 · OS" 라벨 추출 (간단 파서) */
    private String parseDevice(String ua) {
        if (ua == null || ua.isBlank()) return "알 수 없는 기기";
        String u = ua.toLowerCase();
        String browser =
                u.contains("edg")     ? "Edge" :
                u.contains("opr") || u.contains("opera") ? "Opera" :
                u.contains("chrome")  ? "Chrome" :
                u.contains("firefox") ? "Firefox" :
                u.contains("safari")  ? "Safari" : "브라우저";
        String os =
                u.contains("android")                 ? "Android" :
                u.contains("iphone") || u.contains("ipad") ? "iOS" :
                u.contains("windows")                 ? "Windows" :
                u.contains("mac os") || u.contains("macintosh") ? "macOS" :
                u.contains("linux")                   ? "Linux" : "기타";
        return browser + " · " + os;
    }
}
