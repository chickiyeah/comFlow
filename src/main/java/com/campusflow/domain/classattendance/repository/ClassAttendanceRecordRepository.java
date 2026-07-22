package com.campusflow.domain.classattendance.repository;

import com.campusflow.domain.classattendance.entity.ClassAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassAttendanceRecordRepository extends JpaRepository<ClassAttendanceRecord, Long> {
    List<ClassAttendanceRecord> findBySessionIdOrderById(Long sessionId);
    Optional<ClassAttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    void deleteBySessionId(Long sessionId);

    // 학생 본인 출결(클래스 스코프)
    List<ClassAttendanceRecord> findByStudentIdAndSession_ClassRoomIdOrderBySession_DateDesc(Long studentId, Long classRoomId);
}
