package com.campusflow.domain.jobpilot.repository;

import com.campusflow.domain.jobpilot.entity.JobPostingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPostingEntity, Long> {

    /** 같은 학생이 같은 URL을 최근(after) 안에 수집한 적 있으면 캐시로 사용. */
    Optional<JobPostingEntity> findTopByStudentIdAndUrlAndCreatedAtAfterOrderByCreatedAtDesc(
            Long studentId, String url, LocalDateTime after);
}
