package com.campusflow.domain.session.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 접속 세션(기기) 1건. JWT의 jti와 1:1로 매핑되어 원격 로그아웃(세션 무효화)을 가능케 한다.
 * 다기기 동시 로그인 허용 — 활성 세션을 모두 보관하고 사용자가 개별 폐기할 수 있다.
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_session_jti", columnList = "jti"),
        @Index(name = "idx_session_user", columnList = "user_id, active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 40)
    private String jti;            // JWT ID (UUID)

    @Column(length = 120)
    private String device;         // "Chrome · Windows" 등 사람이 읽는 라벨

    @Column(length = 300)
    private String userAgent;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private boolean active;

    @Builder
    public UserSession(User user, String jti, String device, String userAgent, String ip) {
        this.user        = user;
        this.jti         = jti;
        this.device      = device;
        this.userAgent   = userAgent;
        this.ip          = ip;
        this.createdAt   = LocalDateTime.now();
        this.lastSeenAt  = this.createdAt;
        this.active      = true;
    }

    public void touch() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public void revoke() {
        this.active = false;
    }
}
