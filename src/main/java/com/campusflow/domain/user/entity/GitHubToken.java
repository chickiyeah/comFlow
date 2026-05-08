package com.campusflow.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "github_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GitHubToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 255)
    private String token;

    @Builder
    public GitHubToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
