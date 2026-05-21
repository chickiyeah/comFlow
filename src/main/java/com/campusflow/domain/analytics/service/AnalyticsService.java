package com.campusflow.domain.analytics.service;

import com.campusflow.domain.analytics.dto.GradeTrendResponse;
import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.grade.entity.Grade;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final GradeRepository       gradeRepo;
    private final AttendanceRepository  attendanceRepo;
    private final UserRepository        userRepo;
    private final StudentRepository     studentRepo;

    @Cacheable(value = "gradeTrend", key = "#username")
    public GradeTrendResponse getGradeTrend(String username) {
        Student student = getStudent(username);
        List<Grade> grades = gradeRepo.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(student.getId());

        // 학기별 그룹화
        Map<String, List<Grade>> bySemester = grades.stream()
                .collect(Collectors.groupingBy(g -> g.getGradeYear() + "-" + g.getGradeSemester(),
                        LinkedHashMap::new, Collectors.toList()));

        List<GradeTrendResponse.SemesterStat> stats = bySemester.entrySet().stream()
                .map(e -> {
                    List<Grade> sg = e.getValue();
                    int year = sg.get(0).getGradeYear();
                    int sem  = sg.get(0).getGradeSemester();
                    int credits = sg.stream().mapToInt(Grade::getCredits).sum();
                    double totalPoints = sg.stream()
                            .mapToDouble(g -> g.getGradePoint() * g.getCredits()).sum();
                    double gpa = credits > 0 ? Math.round(totalPoints / credits * 100.0) / 100.0 : 0.0;
                    String label = year + "년 " + sem + "학기";
                    return new GradeTrendResponse.SemesterStat(year, sem, gpa, credits, label);
                }).toList();

        Double overallGpa = gradeRepo.calculateGpa(student.getId());
        int totalCredits = grades.stream().mapToInt(Grade::getCredits).sum();

        return new GradeTrendResponse(stats,
                overallGpa != null ? Math.round(overallGpa * 100.0) / 100.0 : 0.0,
                totalCredits);
    }

    @Cacheable(value = "attendanceTrend", key = "#username")
    public Map<String, Object> getAttendanceSummary(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();

        long present = attendanceRepo.countByStudentIdAndStatus(sid, AttendanceStatus.PRESENT);
        long late    = attendanceRepo.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
        long absent  = attendanceRepo.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
        long excused = attendanceRepo.countByStudentIdAndStatus(sid, AttendanceStatus.EXCUSED);
        long total   = present + late + absent + excused;

        double rate = total > 0 ? Math.round((double) present / total * 1000.0) / 10.0 : 100.0;

        List<String> warningSubjects = attendanceRepo.findSubjectsWithAbsenceWarning(sid)
                .stream().map(r -> (String) r[0]).toList();

        return Map.of(
                "present", present, "late", late,
                "absent", absent, "excused", excused,
                "total", total, "attendanceRate", rate,
                "warningSubjects", warningSubjects
        );
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
