package com.campusflow.domain.classroom.dto;

import com.campusflow.domain.classroom.entity.ClassRole;
import com.campusflow.domain.classroom.entity.ClassRoom;

import java.time.LocalDateTime;

public record ClassResponse(
        Long id,
        String code,
        String name,
        String subject,
        String description,
        String ownerName,
        String myRole,
        int memberCount,
        LocalDateTime createdAt
) {
    public static ClassResponse from(ClassRoom c, ClassRole myRole, long memberCount) {
        return new ClassResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getSubject(),
                c.getDescription(),
                c.getOwnerUser().getName(),
                myRole == null ? null : myRole.name(),
                (int) memberCount,
                c.getCreatedAt()
        );
    }
}
