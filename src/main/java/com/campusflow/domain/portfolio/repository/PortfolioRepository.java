package com.campusflow.domain.portfolio.repository;

import com.campusflow.domain.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByStudentIdOrderByStartDateDesc(Long studentId);
    Optional<Portfolio> findByIdAndStudentId(Long id, Long studentId);
}
