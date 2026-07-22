package com.campusflow.domain.kmate.repository;

import com.campusflow.domain.kmate.entity.KmateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KmateHistoryRepository extends JpaRepository<KmateHistory, Long> {
    List<KmateHistory> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
