package com.campusflow.domain.classmeeting.repository;

import com.campusflow.domain.classmeeting.entity.ClassMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassMeetingRepository extends JpaRepository<ClassMeeting, Long> {
    Optional<ClassMeeting> findFirstByClassRoomIdAndActiveTrueOrderByStartedAtDesc(Long classRoomId);
}
