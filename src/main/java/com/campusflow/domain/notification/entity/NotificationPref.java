package com.campusflow.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 알림 수신 설정. 노이즈가 큰 broadcast 계열(채용·공지/강좌)만 끌 수 있고,
 * 학사 경보·시스템(보안/교수 메시지)은 항상 수신한다.
 */
@Entity
@Table(name = "notification_prefs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPref {

    @Id
    private Long userId;            // User PK 공유

    @Column(nullable = false)
    private boolean recvJobAlert;   // 채용 알림(JOB_ALERT)

    @Column(nullable = false)
    private boolean recvNotice;     // 공지·강좌(NOTICE)

    public NotificationPref(Long userId) {
        this.userId = userId;
        this.recvJobAlert = true;
        this.recvNotice = true;
    }

    public void update(boolean jobAlert, boolean notice) {
        this.recvJobAlert = jobAlert;
        this.recvNotice = notice;
    }
}
