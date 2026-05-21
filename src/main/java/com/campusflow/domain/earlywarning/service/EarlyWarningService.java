package com.campusflow.domain.earlywarning.service;

import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.global.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarlyWarningService {

    private final StudentRepository     studentRepository;
    private final AttendanceRepository  attendanceRepository;
    private final GradeRepository       gradeRepository;
    private final MailService           mailService;

    @Transactional(readOnly = true)
    public void checkAndNotifyAll() {
        List<Student> students = studentRepository.findAll();
        int warned = 0;

        for (Student student : students) {
            String email = student.getEmail() != null
                    ? student.getEmail()
                    : (student.getUser() != null ? student.getUser().getEmail() : null);
            if (email == null || email.isBlank()) continue;

            List<String> warnings = detectWarnings(student);
            if (!warnings.isEmpty()) {
                sendWarningMail(email, student, warnings);
                warned++;
            }
        }
        log.info("[조기경보] 검사 완료 — 전체 {}명 중 {}명 경고 발송", students.size(), warned);
    }

    private List<String> detectWarnings(Student student) {
        Long sid = student.getId();
        List<String> warnings = new ArrayList<>();

        // 출석 분석
        long present  = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.PRESENT);
        long late     = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
        long absent   = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
        long excused  = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.EXCUSED);
        long total    = present + late + absent + excused;

        if (total > 0) {
            double attendanceRate = (double) present / total * 100;
            if (attendanceRate < 60) {
                warnings.add(String.format("🚨 출석률 심각: %.1f%% (60%% 미만)", attendanceRate));
            } else if (attendanceRate < 70) {
                warnings.add(String.format("⚠️ 출석률 위험: %.1f%% (70%% 미만)", attendanceRate));
            }
        }

        // 결석 경고 과목
        List<String> absenceSubjects = attendanceRepository.findSubjectsWithAbsenceWarning(sid)
                .stream().map(r -> (String) r[0]).toList();
        if (!absenceSubjects.isEmpty()) {
            warnings.add("📚 결석 3회 이상 과목: " + String.join(", ", absenceSubjects));
        }

        // GPA 확인
        Double gpa = gradeRepository.calculateGpa(sid);
        if (gpa != null && gpa < 2.0) {
            warnings.add(String.format("📉 학점 위험: GPA %.2f (2.0 미만)", gpa));
        }

        return warnings;
    }

    private void sendWarningMail(String email, Student student, List<String> warnings) {
        String warningItems = warnings.stream()
                .map(w -> "<li style='margin:8px 0'>" + w + "</li>")
                .reduce("", String::concat);

        String html = """
                <div style="font-family:sans-serif;max-width:600px;margin:auto;padding:24px">
                  <h2 style="color:#00236f">📢 학업 현황 알림 — CampusFlow</h2>
                  <p>안녕하세요, <strong>%s</strong> 학생.</p>
                  <p>학업 현황 점검 결과 아래 항목에서 주의가 필요합니다.</p>
                  <ul style="background:#fff3cd;border-left:4px solid #ffc107;padding:16px 16px 16px 32px;border-radius:4px">
                    %s
                  </ul>
                  <p>CampusFlow에서 자세한 현황을 확인하고 담당 교수님께 상담을 요청해보세요.</p>
                  <p style="color:#888;font-size:12px">이 메일은 자동 발송됩니다.</p>
                </div>
                """.formatted(student.getName(), warningItems);

        mailService.send(email, "학업 현황 알림", html);
    }
}
