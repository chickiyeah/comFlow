package com.campusflow.domain.resume.entity;

import com.campusflow.domain.portfolio.entity.Portfolio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumePortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false)
    private int displayOrder; // 이력서 내 표시 순서

    public ResumePortfolio(Resume resume, Portfolio portfolio, int displayOrder) {
        this.resume = resume;
        this.portfolio = portfolio;
        this.displayOrder = displayOrder;
    }
}
