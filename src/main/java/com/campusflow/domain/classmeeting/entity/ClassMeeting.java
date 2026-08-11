package com.campusflow.domain.classmeeting.entity;

import com.campusflow.domain.classroom.entity.ClassRoom;
import com.campusflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 클래스 화상 미팅(Jitsi 방 URL). 실제 Jitsi 서버 연동 없이 방 URL 생성 + 활성 플래그만 관리. */
@Entity
@Table(name = "class_meetings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassRoom classRoom;

    @Column(nullable = false, length = 500)
    private String roomUrl;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Builder
    public ClassMeeting(ClassRoom classRoom, String roomUrl, boolean active, User startedBy, LocalDateTime startedAt) {
        this.classRoom = classRoom;
        this.roomUrl = roomUrl;
        this.active = active;
        this.startedBy = startedBy;
        this.startedAt = startedAt;
    }

    public void end(LocalDateTime endedAt) {
        this.active = false;
        this.endedAt = endedAt;
    }
}
