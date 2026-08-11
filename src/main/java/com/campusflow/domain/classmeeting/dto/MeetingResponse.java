package com.campusflow.domain.classmeeting.dto;

import com.campusflow.domain.classmeeting.entity.ClassMeeting;

import java.time.LocalDateTime;

public record MeetingResponse(
        Long id,
        String roomUrl,
        boolean active,
        String startedByName,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static MeetingResponse from(ClassMeeting m) {
        return new MeetingResponse(
                m.getId(), m.getRoomUrl(), m.isActive(),
                m.getStartedBy() != null ? m.getStartedBy().getName() : null,
                m.getStartedAt(), m.getEndedAt()
        );
    }
}
