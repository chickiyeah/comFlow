package com.campusflow.domain.classroom.dto;

import com.campusflow.domain.classroom.entity.ClassMember;
import com.campusflow.domain.user.entity.User;

import java.time.LocalDateTime;

public record ClassMemberResponse(
        Long userId,
        String username,
        String name,
        String role,
        LocalDateTime joinedAt
) {
    public static ClassMemberResponse from(ClassMember m) {
        User u = m.getUser();
        return new ClassMemberResponse(u.getId(), u.getUsername(), u.getName(), m.getRole().name(), m.getJoinedAt());
    }
}
