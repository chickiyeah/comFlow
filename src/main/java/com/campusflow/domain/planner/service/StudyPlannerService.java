package com.campusflow.domain.planner.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.planner.dto.GradePredictRequest;
import com.campusflow.domain.planner.dto.StudyPlanRequest;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyPlannerService {

    private final AiFacadeService  aiFacade;
    private final GradeRepository  gradeRepo;
    private final UserRepository   userRepo;
    private final StudentRepository studentRepo;

    private static final String PLANNER_SYSTEM = """
            당신은 대학생 학습 플래너 전문가입니다.
            학생의 시험 일정과 취약 과목을 고려해 실현 가능한 주간 공부 계획을 세워주세요.
            반드시 JSON 형식으로만 응답하세요:
            {
              "weeklyPlan": [
                {"date": "YYYY-MM-DD", "subject": "과목명", "task": "할 일", "hours": 2}
              ],
              "tips": ["팁1", "팁2"]
            }
            """;

    public Map<String, Object> generatePlan(String username, StudyPlanRequest req) {
        Student student = getStudent(username);
        Double gpa = gradeRepo.calculateGpa(student.getId());

        String examsText = req.exams() == null ? "없음" :
                req.exams().stream()
                        .map(e -> e.subject() + " (" + e.examDate() + ")")
                        .collect(Collectors.joining(", "));

        String weakText = req.weakSubjects() == null ? "없음" :
                String.join(", ", req.weakSubjects());

        String prompt = """
                학생 정보:
                - 현재 GPA: %.2f
                - 시험 일정: %s
                - 취약 과목: %s

                위 정보를 바탕으로 오늘부터 가장 가까운 시험까지의 학습 계획을 세워주세요.
                하루 최대 8시간 이내로 현실적으로 계획하세요.
                """.formatted(gpa != null ? gpa : 0.0, examsText, weakText);

        String raw = aiFacade.ask(PLANNER_SYSTEM, prompt);
        // JSON 파싱 후 Map으로 반환 (파싱 실패 시 raw 반환)
        try {
            String json = raw.trim()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```\\s*$", "").trim();
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("weeklyPlan", java.util.List.of(), "tips", java.util.List.of(raw));
        }
    }

    public Map<String, Object> predictGrade(String username, GradePredictRequest req) {
        // 현재 예상 점수 = 중간고사 기여분
        double midContrib = req.midtermScore() * (req.midtermWeight() / 100.0);

        // 목표 학점별 필요 기말 점수
        double[] targetScores = {90, 80, 70, 60};
        String[] letterGrades = {"A+/A", "B+/B", "C+/C", "D"};
        java.util.List<Map<String, Object>> scenarios = new java.util.ArrayList<>();

        for (int i = 0; i < targetScores.length; i++) {
            double finalNeeded = (targetScores[i] - midContrib) / (req.finalWeight() / 100.0);
            scenarios.add(Map.of(
                    "targetGrade", letterGrades[i],
                    "targetTotal", targetScores[i],
                    "finalNeeded", Math.max(0, Math.min(100, Math.round(finalNeeded * 10.0) / 10.0))
            ));
        }

        // 현재 예상 최종 점수 (기말 평균 60점 가정)
        double estimatedFinal = midContrib + 60 * (req.finalWeight() / 100.0);

        // 출석 패널티 경고
        String attendanceWarning = req.attendanceRate() < 70
                ? "⚠️ 출석률 " + req.attendanceRate() + "% — 출석 점수 감점 가능"
                : null;

        return Map.of(
                "subjectName",    req.subjectName(),
                "midtermScore",   req.midtermScore(),
                "estimatedTotal", Math.round(estimatedFinal * 10.0) / 10.0,
                "scenarios",      scenarios,
                "attendanceWarning", attendanceWarning != null ? attendanceWarning : ""
        );
    }

    private Student getStudent(String username) {
        Long userId = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND)).getId();
        return studentRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
