package com.campusflow.domain.session.repository;

import com.campusflow.domain.session.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByJti(String jti);

    List<UserSession> findByUserIdAndActiveTrueOrderByLastSeenAtDesc(Long userId);

    long countByUserIdAndActiveTrue(Long userId);
}
