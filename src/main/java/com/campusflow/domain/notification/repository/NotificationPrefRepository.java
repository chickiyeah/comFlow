package com.campusflow.domain.notification.repository;

import com.campusflow.domain.notification.entity.NotificationPref;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPrefRepository extends JpaRepository<NotificationPref, Long> {
    Optional<NotificationPref> findByUserId(Long userId);
}
