package com.campusflow.domain.push.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 브라우저 푸시 구독 1건 (Web Push). endpoint + p256dh + auth 키로 VAPID 푸시 발송. */
@Entity
@Table(name = "push_subscriptions", indexes = @Index(name = "idx_push_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user      = user;
        this.endpoint  = endpoint;
        this.p256dh    = p256dh;
        this.auth      = auth;
        this.createdAt = LocalDateTime.now();
    }
}
