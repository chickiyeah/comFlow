package com.campusflow.domain.assistant.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.assistant.dto.AssistantResponse;
import com.campusflow.domain.attendance.entity.AttendanceStatus;
import com.campusflow.domain.attendance.repository.AttendanceRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.graduation.repository.GraduationRequirementRepository;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssistantService {

    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final GraduationRequirementRepository requirementRepository;
    private final PortfolioRepository portfolioRepository;

    private static final String SYSTEM_PROMPT = """
            당신은 2년제 컴퓨터정보과 학생 전담 AI 어드바이저입니다.
            학생의 실제 학업 데이터를 분석하여 구체적이고 실용적인 조언을 제공하세요.
            반드시 아래 JSON 형식으로만 응답하세요. 마크다운 없이 JSON만 출력하세요.

            {
              "overallAdvice": "종합 한줄 평가 (1-2문장, 현재 상황과 핵심 조언)",
              "studyFocus": ["다음 학기 집중 과목1", "과목2"],
              "certificates": [
                {"name": "자격증명", "reason": "추천 이유", "priority": "긴급|권장|선택"}
              ],
              "portfolioIdeas": ["프로젝트 아이디어1", "아이디어2", "아이디어3"],
              "attendanceAdvice": "출결 관련 조언 (문제 없으면 null)"
            }
            """;

    public AssistantResponse analyze(String username) {
        Student student = getStudentByUsername(username);
        Long sid = student.getId();

        // 1. 성적 데이터 수집
        var grades = gradeRepository.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(sid);
        Double gpa = gradeRepository.calculateGpa(sid);
        int totalCredits = grades.stream().mapToInt(g -> g.getCredits()).sum();
        String gradesSummary = grades.stream()
                .map(g -> String.format("%s(%s,%.1f)", g.getSubjectName(), g.getLetterGrade(), g.getGradePoint()))
                .collect(Collectors.joining(", "));

        // 2. 출결 데이터 수집
        long absences = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.ABSENT);
        long lates = attendanceRepository.countByStudentIdAndStatus(sid, AttendanceStatus.LATE);
        var absenceWarnings = attendanceRepository.findSubjectsWithAbsenceWarning(sid)
                .stream().map(r -> (String) r[0]).toList();

        // 3. 졸업요건 수집
        var requirements = requirementRepository.findByDepartment(student.getDepartment());
        String gradStatus = requirements.isEmpty()
                ? "졸업요건 데이터 없음"
                : requirements.stream()
                    .map(r -> String.format("%s(%s): %d/%d학점",
                            r.getName(), r.getCategory(), 0, r.getRequiredCredits()))
                    .collect(Collectors.joining(", "));

        // 4. 포트폴리오 수
        int portfolioCount = portfolioRepository.findByStudentIdOrderByStartDateDesc(sid).size();

        // 현재 학기 계산 (학년·학기 기반)
        int currentSemester = (student.getGrade() - 1) * 2 + student.getSemester();
        int remainingSemesters = Math.max(0, 4 - currentSemester);

        // 5. AI에 전달할 컨텍스트 구성
        String context = String.format("""
                학생 정보:
                - 이름: %s
                - 학과: %s
                - 현재 %d학년 %d학기 (전체 4학기 중 %d학기 차, 남은 학기: %d)
                - 누적 GPA: %.2f / 4.5
                - 이수 학점: %d학점
                - 성적 내역: %s
                - 결석: %d회, 지각: %d회
                - 결석 경고 과목: %s
                - 졸업요건 현황: %s
                - 등록된 포트폴리오: %d개
                """,
                student.getName(), student.getDepartment(),
                student.getGrade(), student.getSemester(),
                currentSemester, remainingSemesters,
                gpa != null ? gpa : 0.0, totalCredits,
                gradesSummary.isEmpty() ? "없음" : gradesSummary,
                absences, lates,
                absenceWarnings.isEmpty() ? "없음" : String.join(", ", absenceWarnings),
                gradStatus,
                portfolioCount
        );

        // 6. AI 호출
        String raw = aiFacadeService.ask(SYSTEM_PROMPT,
                "다음 학생 데이터를 분석하여 맞춤 추천을 JSON으로 제공하세요:\n\n" + context);

        return parseResponse(raw, student, gpa, currentSemester, remainingSemesters, absenceWarnings, requirements);
    }

    @SuppressWarnings("unchecked")
    private AssistantResponse parseResponse(String raw, Student student, Double gpa,
                                             int currentSemester, int remainingSemesters,
                                             List<String> absenceWarnings,
                                             List<?> requirements) {
        try {
            String json = raw.trim().replaceAll("(?s)```json?\\s*", "").replaceAll("```\\s*$", "").trim();
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

            String overallAdvice = (String) map.getOrDefault("overallAdvice", "분석 중입니다.");
            List<String> studyFocus = (List<String>) map.getOrDefault("studyFocus", List.of());
            List<String> portfolioIdeas = (List<String>) map.getOrDefault("portfolioIdeas", List.of());

            List<AssistantResponse.CertRecommend> certs = ((List<Map<String, String>>)
                    map.getOrDefault("certificates", List.of()))
                    .stream()
                    .map(c -> new AssistantResponse.CertRecommend(
                            c.getOrDefault("name", ""),
                            c.getOrDefault("reason", ""),
                            c.getOrDefault("priority", "권장")))
                    .toList();

            List<AssistantResponse.GradWarning> gradWarnings = requirements.stream()
                    .filter(r -> {
                        var req = (com.campusflow.domain.graduation.entity.GraduationRequirement) r;
                        return req.getRequiredCredits() > 0;
                    })
                    .map(r -> {
                        var req = (com.campusflow.domain.graduation.entity.GraduationRequirement) r;
                        return new AssistantResponse.GradWarning(
                                req.getCategory(), 0, req.getRequiredCredits(), req.getRequiredCredits());
                    })
                    .toList();

            List<String> attWarnings = absenceWarnings.stream()
                    .map(s -> s + " 결석 3회 이상 — 수료 위험")
                    .toList();

            return new AssistantResponse(
                    student.getName(), currentSemester, remainingSemesters,
                    gpa != null ? Math.round(gpa * 100.0) / 100.0 : 0.0,
                    overallAdvice, studyFocus, certs, portfolioIdeas, gradWarnings, attWarnings
            );
        } catch (Exception e) {
            log.warn("AI 응답 파싱 실패: {}", e.getMessage());
            return new AssistantResponse(student.getName(), currentSemester, remainingSemesters,
                    gpa != null ? gpa : 0.0,
                    "데이터 분석 중 오류가 발생했습니다.", List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
