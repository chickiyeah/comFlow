package com.campusflow.domain.grade.service;

import com.campusflow.domain.grade.dto.GradeResponse;
import com.campusflow.domain.grade.dto.GradeSummaryResponse;
import com.campusflow.domain.grade.entity.Grade;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeService {

    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public GradeSummaryResponse getMyGrades(String username) {
        Student student = getStudentByUsername(username);
        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(student.getId());
        Double gpa = gradeRepository.calculateGpa(student.getId());
        int totalCredits = grades.stream().mapToInt(Grade::getCredits).sum();

        return new GradeSummaryResponse(
                gpa != null ? Math.round(gpa * 100.0) / 100.0 : 0.0,
                totalCredits,
                grades.stream().map(GradeResponse::from).toList()
        );
    }

    /** 성적증명서 PDF용 모델 */
    public Map<String, Object> getTranscriptModel(String username) {
        Student student = getStudentByUsername(username);
        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(student.getId());
        Double gpa = gradeRepository.calculateGpa(student.getId());
        int totalCredits = grades.stream().mapToInt(Grade::getCredits).sum();

        Map<String, Object> m = new HashMap<>();
        m.put("docTitle", "성적증명서");
        m.put("name", student.getName());
        m.put("studentId", student.getStudentId());
        m.put("department", student.getDepartment());
        m.put("gradeSemester", student.getGrade() + "학년 " + student.getSemester() + "학기");
        m.put("grades", grades.stream().map(GradeResponse::from).toList());
        m.put("gpa", gpa != null ? Math.round(gpa * 100.0) / 100.0 : 0.0);
        m.put("totalCredits", totalCredits);
        m.put("issuedDate", LocalDate.now().toString());
        return m;
    }

    public List<GradeResponse> getGradesBySemester(String username, int year, int semester) {
        Student student = getStudentByUsername(username);
        return gradeRepository.findByStudentIdAndGradeYearAndGradeSemester(student.getId(), year, semester)
                .stream().map(GradeResponse::from).toList();
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
