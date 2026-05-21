package com.campusflow.domain.study.dto;

import com.campusflow.domain.study.entity.StudyGroup;

import java.time.LocalDateTime;

public record StudyGroupResponse(
        Long id,
        String name,
        String subject,
        String description,
        int maxMembers,
        int currentMembers,
        String leaderName,
        String status,
        boolean isMember,
        boolean isLeader,
        LocalDateTime createdAt
) {
    public static StudyGroupResponse from(StudyGroup g, int currentMembers, Long myStudentId) {
        boolean isLeader = g.getLeader().getId().equals(myStudentId);
        return new StudyGroupResponse(
                g.getId(), g.getName(), g.getSubject(), g.getDescription(),
                g.getMaxMembers(), currentMembers, g.getLeader().getName(),
                g.getStatus().name(), false, isLeader, g.getCreatedAt()
        );
    }

    public StudyGroupResponse withMembership(boolean isMember) {
        return new StudyGroupResponse(id, name, subject, description, maxMembers,
                currentMembers, leaderName, status, isMember, isLeader, createdAt);
    }
}
