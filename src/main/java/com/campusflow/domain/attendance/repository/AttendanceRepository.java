package com.campusflow.domain.attendance.repository;

import com.campusflow.domain.attendance.entity.Attendance;
import com.campusflow.domain.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentIdOrderByAttendanceDateDesc(Long studentId);

    List<Attendance> findByStudentIdAndSubjectName(Long studentId, String subjectName);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = :status")
    long countByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") AttendanceStatus status);

    // 결석 3회 이상 경고 체크
    @Query("SELECT a.subjectName, COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'ABSENT' GROUP BY a.subjectName HAVING COUNT(a) >= 3")
    List<Object[]> findSubjectsWithAbsenceWarning(@Param("studentId") Long studentId);
}
