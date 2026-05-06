package com.campusflow.domain.graduation.service;

import com.campusflow.domain.graduation.dto.GraduationCheckResponse;
import com.campusflow.domain.graduation.dto.GraduationCheckResponse.RequirementStatus;
import com.campusflow.domain.graduation.entity.GraduationRequirement;
import com.campusflow.domain.graduation.repository.GraduationRequirementRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationService {

    private static final int REQUIRED_TOTAL_CREDITS = 80; // 2년제 졸업 요건
    private static final double REQUIRED_MIN_GPA = 2.0;

    private final GraduationRequirementRepository requirementRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public GraduationCheckResponse checkGraduation(String username) {
        Student student = getStudentByUsername(username);
        Long sid = student.getId();

        List<Grade> grades = gradeRepository.findByStudentIdOrderByGradeYearAscGradeSemesterAsc(sid);
        Double gpa = gradeRepository.calculateGpa(sid);
        int totalCredits = grades.stream().mapToInt(Grade::getCredits).sum();

        // 과목별 이수 학점 집계 (카테고리 매핑은 실제 교육과정에 맞게 확장)
        Map<String, Integer> earnedBySubject = grades.stream()
                .collect(Collectors.groupingBy(Grade::getSubjectName,
                        Collectors.summingInt(Grade::getCredits)));

        List<GraduationRequirement> requirements = requirementRepository.findByDepartment(student.getDepartment());

        List<RequirementStatus> statuses = requirements.stream().map(req -> {
            int earned = earnedBySubject.getOrDefault(req.getName(), 0);
            boolean completed = earned >= req.getRequiredCredits();
            return new RequirementStatus(req.getCategory(), req.getName(), earned, req.getRequiredCredits(), completed);
        }).toList();

        boolean eligible = totalCredits >= REQUIRED_TOTAL_CREDITS
                && (gpa != null && gpa >= REQUIRED_MIN_GPA)
                && statuses.stream().allMatch(RequirementStatus::completed);

        return new GraduationCheckResponse(
                eligible,
                gpa != null ? Math.round(gpa * 100.0) / 100.0 : 0.0,
                totalCredits,
                REQUIRED_TOTAL_CREDITS,
                statuses
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
