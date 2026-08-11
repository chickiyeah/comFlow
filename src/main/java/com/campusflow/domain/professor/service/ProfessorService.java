package com.campusflow.domain.professor.service;

import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.grade.entity.Grade;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.professor.dto.ProfessorOverviewResponse;
import com.campusflow.domain.professor.dto.ProfessorStudentDetailResponse;
import com.campusflow.domain.professor.dto.ProfessorStudentRow;
import com.campusflow.domain.professor.dto.ProfessorAnalyticsResponse;
import com.campusflow.domain.course.repository.CourseViewRepository;
import com.campusflow.domain.quiz.entity.QuizSubmission;
import com.campusflow.domain.quiz.repository.QuizSubmissionRepository;
import com.campusflow.domain.notification.entity.NotificationType;
import com.campusflow.domain.notification.service.NotificationService;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private static final double GPA_RISK_THRESHOLD = 2.5;

    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final CourseViewRepository courseViewRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    public ProfessorOverviewResponse overview() {
        List<Student> all = studentRepository.findAll();
        double gpaSum = 0;
        int gpaCount = 0;
        long atRisk = 0;
        for (Student s : all) {
            Double gpa = gradeRepository.calculateGpa(s.getId());
            boolean warn = !attendanceRepository.findSubjectsWithAbsenceWarning(s.getId()).isEmpty();
            if (gpa != null) { gpaSum += gpa; gpaCount++; }
            if (warn || (gpa != null && gpa < GPA_RISK_THRESHOLD)) atRisk++;
        }
        double avg = gpaCount > 0 ? round2(gpaSum / gpaCount) : 0;
        return new ProfessorOverviewResponse(all.size(), avg, atRisk);
    }

    public List<ProfessorStudentRow> students() {
        return studentRepository.findAll().stream()
                .map(this::toRow)
                .sorted((a, b) -> Boolean.compare(b.atRisk(), a.atRisk())) // 위험군 먼저
                .toList();
    }

    private ProfessorStudentRow toRow(Student s) {
        Double gpa = gradeRepository.calculateGpa(s.getId());
        long present = attendanceRepository.countByStudentIdAndStatus(s.getId(), AttendanceStatus.PRESENT);
        long late    = attendanceRepository.countByStudentIdAndStatus(s.getId(), AttendanceStatus.LATE);
        long absent  = attendanceRepository.countByStudentIdAndStatus(s.getId(), AttendanceStatus.ABSENT);
        long denom = present + late + absent;
        Integer rate = denom > 0 ? (int) Math.round(present * 100.0 / denom) : null;
        boolean warn = !attendanceRepository.findSubjectsWithAbsenceWarning(s.getId()).isEmpty();
        boolean atRisk = warn || (gpa != null && gpa < GPA_RISK_THRESHOLD);
        return new ProfessorStudentRow(
                s.getId(), s.getStudentId(), s.getName(), s.getGrade(), s.getSemester(),
                s.getDepartment(), gpa != null ? round2(gpa) : 0, rate, atRisk);
    }

    public ProfessorStudentDetailResponse detail(Long studentId) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(studentId);
        Double gpa = gradeRepository.calculateGpa(studentId);
        int totalCredits = grades.stream().mapToInt(Grade::getCredits).sum();

        List<ProfessorStudentDetailResponse.GradeItem> gradeItems = grades.stream()
                .map(g -> new ProfessorStudentDetailResponse.GradeItem(
                        g.getSubjectName(), g.getSubjectCode(), g.getCredits(),
                        g.getLetterGrade(), g.getGradePoint(), g.getGradeYear(), g.getGradeSemester()))
                .toList();

        long present = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);
        long late    = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.LATE);
        long absent  = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);
        long excused = attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.EXCUSED);
        long denom = present + late + absent;
        Integer rate = denom > 0 ? (int) Math.round(present * 100.0 / denom) : null;
        List<String> warnings = attendanceRepository.findSubjectsWithAbsenceWarning(studentId).stream()
                .map(o -> (String) o[0]).toList();

        var attendance = new ProfessorStudentDetailResponse.AttendanceSummary(
                present, late, absent, excused, rate, warnings);

        return new ProfessorStudentDetailResponse(
                s.getStudentId(), s.getName(), s.getGrade(), s.getSemester(), s.getDepartment(),
                s.getPhone(), s.getEmail(),
                gpa != null ? round2(gpa) : 0, totalCredits, gradeItems, attendance);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ── 학습분석/조기경보 대시보드 ──────────────────────────────
    public ProfessorAnalyticsResponse analytics() {
        List<Student> all = studentRepository.findAll();
        List<ProfessorAnalyticsResponse.StudentAnalytics> rows = new java.util.ArrayList<>();
        double gpaSum = 0; int gpaCnt = 0, attSum = 0, attCnt = 0, quizSumAll = 0, quizCntAll = 0;
        long highRisk = 0, medRisk = 0, completionsAll = 0;

        for (Student s : all) {
            Long sid = s.getId();
            Double gpaObj = gradeRepository.calculateGpa(sid);
            double gpa = gpaObj != null ? round2(gpaObj) : 0.0;

            long present = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.PRESENT);
            long late    = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
            long absent  = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
            long denom = present + late + absent;
            Integer rate = denom > 0 ? (int) Math.round(present * 100.0 / denom) : null;
            boolean warn = !attendanceRepository.findSubjectsWithAbsenceWarning(sid).isEmpty();

            int completions = (int) courseViewRepository.findByStudentId(sid).stream()
                    .filter(com.campusflow.domain.course.entity.CourseView::isCompleted).count();
            completionsAll += completions;

            List<QuizSubmission> subs = quizSubmissionRepository.findByStudentId(sid);
            Integer quizAvg = null;
            if (!subs.isEmpty()) {
                double pctSum = subs.stream().filter(q -> q.getMaxScore() > 0)
                        .mapToDouble(q -> q.getScore() * 100.0 / q.getMaxScore()).average().orElse(0);
                quizAvg = (int) Math.round(pctSum);
            }

            // 위험도 점수 (리서치 강예측인자: 성적·출석·참여·평가)
            int score = 0;
            List<String> reasons = new java.util.ArrayList<>();
            if (gpaObj != null) {
                if (gpa < 2.0)      { score += 40; reasons.add("LOW_GPA"); }
                else if (gpa < 2.5) { score += 25; reasons.add("LOW_GPA"); }
                else if (gpa < 3.0) { score += 10; }
            }
            if (warn)                       { score += 30; reasons.add("ATTENDANCE"); }
            else if (rate != null && rate < 70) { score += 20; reasons.add("ATTENDANCE"); }
            if (completions == 0)           { score += 15; reasons.add("NO_ENGAGEMENT"); }
            if (quizAvg != null && quizAvg < 60) { score += 15; reasons.add("LOW_QUIZ"); }
            score = Math.min(100, score);
            String level = score >= 50 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
            if ("HIGH".equals(level)) highRisk++; else if ("MEDIUM".equals(level)) medRisk++;

            if (gpaObj != null) { gpaSum += gpa; gpaCnt++; }
            if (rate != null)   { attSum += rate; attCnt++; }
            if (quizAvg != null){ quizSumAll += quizAvg; quizCntAll++; }

            rows.add(new ProfessorAnalyticsResponse.StudentAnalytics(
                    sid, s.getStudentId(), s.getName(), s.getGrade(), s.getSemester(),
                    gpa, rate, completions, quizAvg, score, level, reasons));
        }
        rows.sort((a, b) -> Integer.compare(b.riskScore(), a.riskScore())); // 위험 높은 순

        var agg = new ProfessorAnalyticsResponse.Aggregate(
                all.size(),
                gpaCnt > 0 ? round2(gpaSum / gpaCnt) : 0.0,
                highRisk, medRisk,
                attCnt > 0 ? Math.round((float) attSum / attCnt) : null,
                completionsAll,
                quizCntAll > 0 ? Math.round((float) quizSumAll / quizCntAll) : null);
        return new ProfessorAnalyticsResponse(agg, rows);
    }

    // ── 학생 알림 발송 (인앱 + 웹푸시 자동) ──────────────────────
    @Transactional
    public void notifyStudent(Long studentId, String title, String message) {
        if (message == null || message.isBlank()) throw new BusinessException(ErrorCode.INVALID_INPUT);
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        notificationService.create(s, NotificationType.SYSTEM,
                (title != null && !title.isBlank()) ? title : "교수님 메시지", message, "/");
    }

    /** 위험군(출결 경고 또는 GPA 미달) 전체에게 알림 발송. 발송 수 반환. */
    @Transactional
    public int notifyAtRisk(String title, String message) {
        if (message == null || message.isBlank()) throw new BusinessException(ErrorCode.INVALID_INPUT);
        int count = 0;
        for (Student s : studentRepository.findAll()) {
            Double gpa = gradeRepository.calculateGpa(s.getId());
            boolean warn = !attendanceRepository.findSubjectsWithAbsenceWarning(s.getId()).isEmpty();
            if (warn || (gpa != null && gpa < GPA_RISK_THRESHOLD)) {
                notificationService.create(s, NotificationType.SYSTEM,
                        (title != null && !title.isBlank()) ? title : "학습 상담 안내", message, "/");
                count++;
            }
        }
        return count;
    }
}
