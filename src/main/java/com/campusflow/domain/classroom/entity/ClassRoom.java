package com.campusflow.domain.classroom.entity;

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
 * 클래스(수업). NovaClass의 {@code classes} 대응. 개설자는 {@code ownerUser}이며 별도의 {@link ClassMember}(OWNER) 행도 갖는다.
 */
@Entity
@Table(name = "classes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String code; // 6자리 참여 코드

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String subject;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ClassRoom(String code, String name, String subject, String description, User ownerUser) {
        this.code = code;
        this.name = name;
        this.subject = subject;
        this.description = description;
        this.ownerUser = ownerUser;
    }

    public void update(String name, String subject, String description) {
        this.name = name;
        this.subject = subject;
        this.description = description;
    }
}
