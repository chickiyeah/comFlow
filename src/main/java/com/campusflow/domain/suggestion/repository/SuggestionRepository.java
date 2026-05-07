package com.campusflow.domain.suggestion.repository;

import com.campusflow.domain.suggestion.entity.Suggestion;
import com.campusflow.domain.suggestion.entity.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    Page<Suggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status, Pageable pageable);
    Page<Suggestion> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
