package com.campusflow.domain.classattendance.repository;

import com.campusflow.domain.classattendance.entity.ClassAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassAttendanceSessionRepository extends JpaRepository<ClassAttendanceSession, Long> {
    List<ClassAttendanceSession> findByClassRoomIdOrderByDateDesc(Long classRoomId);
}
