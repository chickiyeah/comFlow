package com.campusflow.domain.attendance.service;

import com.campusflow.domain.attendance.dto.AttendanceResponse;
import com.campusflow.domain.attendance.dto.AttendanceSummaryResponse;
import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public AttendanceSummaryResponse getMySummary(String username) {
        Student student = getStudentByUsername(username);
        Long sid = student.getId();

        var records = attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(sid);
        long present = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.PRESENT);
        long late = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
        long absent = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
        long excused = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.EXCUSED);

        List<String> warnings = attendanceRepository.findSubjectsWithAbsenceWarning(sid)
                .stream().map(row -> (String) row[0]).toList();

        return new AttendanceSummaryResponse(
                present, late, absent, excused, warnings,
                records.stream().map(AttendanceResponse::from).toList()
        );
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
