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
 * 클래스 멤버십. (classRoom, user) 유일. 개설자도 OWNER 역할의 행을 갖는다 → 멤버 조회가 일관됨.
 */
@Entity
@Table(name = "class_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_member", columnNames = {"class_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClassMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassRole role;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public ClassMember(ClassRoom classRoom, User user, ClassRole role) {
        this.classRoom = classRoom;
        this.user = user;
        this.role = role;
    }

    public void changeRole(ClassRole role) {
        this.role = role;
    }
}
