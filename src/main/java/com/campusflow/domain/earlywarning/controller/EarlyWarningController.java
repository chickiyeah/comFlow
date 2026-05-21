package com.campusflow.domain.earlywarning.controller;

import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.campusflow.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warning")
@RequiredArgsConstructor
public class EarlyWarningController {

    private final UserRepository        userRepository;
    private final StudentRepository     studentRepository;
    private final AttendanceRepository  attendanceRepository;
    private final GradeRepository       gradeRepository;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMyWarnings(@AuthenticationPrincipal String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        Long sid = student.getId();

        long present = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.PRESENT);
        long late    = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
        long absent  = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
        long excused = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.EXCUSED);
        long total   = present + late + absent + excused;

        double rate  = total > 0 ? Math.round((double) present / total * 1000.0) / 10.0 : 100.0;

        Double gpa   = gradeRepository.calculateGpa(sid);

        List<String> absenceWarnings = attendanceRepository.findSubjectsWithAbsenceWarning(sid)
                .stream().map(r -> (String) r[0]).toList();

        List<String> warnings = new ArrayList<>();
        String level = "SAFE";

        if (total > 0 && rate < 60) { warnings.add("출석률 " + rate + "% (심각)"); level = "DANGER"; }
        else if (total > 0 && rate < 70) { warnings.add("출석률 " + rate + "% (위험)"); level = "WARNING"; }

        if (!absenceWarnings.isEmpty()) {
            warnings.add("결석 3회 이상: " + String.join(", ", absenceWarnings));
            if (level.equals("SAFE")) level = "WARNING";
        }

        if (gpa != null && gpa < 2.0) {
            warnings.add("GPA " + String.format("%.2f", gpa) + " (2.0 미만)");
            level = "DANGER";
        }

        return ApiResponse.ok(Map.of(
                "level",    level,
                "warnings", warnings,
                "attendanceRate", rate,
                "absenceCount",   absent,
                "gpa", gpa != null ? gpa : 0.0
        ));
    }
}
