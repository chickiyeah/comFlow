package com.campusflow.domain.graduation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "graduation_requirements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GraduationRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(nullable = false, length = 50)
    private String category; // 전공필수, 전공선택, 교양필수, 자격증 등

    @Column(nullable = false, length = 100)
    private String name; // 요건명

    @Column(nullable = false)
    private int requiredCredits; // 필요 학점 (자격증은 0)

    @Column(length = 200)
    private String description;

    @Builder
    public GraduationRequirement(String department, String category, String name,
                                  int requiredCredits, String description) {
        this.department = department;
        this.category = category;
        this.name = name;
        this.requiredCredits = requiredCredits;
        this.description = description;
    }
}
