package com.campusflow.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session", indexes = {
        @Index(name = "idx_chat_session_user", columnList = "userId, updatedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 컴정이로 보내는 session_id (대화 컨텍스트 키) */
    @Column(nullable = false, length = 100, unique = true)
    private String sessionKey;

    @Column(length = 200)
    private String title;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public ChatSession(Long userId, String sessionKey, String title) {
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.title = title;
    }

    public void updateTitle(String title) {
        if (title != null && !title.isBlank()) this.title = title;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
