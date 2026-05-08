package com.campusflow.domain.user.repository;

import com.campusflow.domain.user.entity.GitHubToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitHubTokenRepository extends JpaRepository<GitHubToken, Long> {
    Optional<GitHubToken> findByUserId(Long userId);
}
