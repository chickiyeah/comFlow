package com.campusflow.domain.material.entity;

import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 자료(PDF) 페이지 북마크. (material, user, page) 유일.
 */
@Entity
@Table(name = "material_page_bookmarks",
        uniqueConstraints = @UniqueConstraint(name = "uk_material_bookmark",
                columnNames = {"material_id", "user_id", "page_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MaterialBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "page_number", nullable = false)
    private int page;

    @Column(length = 255)
    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MaterialBookmark(Material material, User user, int page, String note) {
        this.material = material;
        this.user = user;
        this.page = page;
        this.note = note;
    }

    public void updateNote(String note) {
        this.note = note;
    }
}
