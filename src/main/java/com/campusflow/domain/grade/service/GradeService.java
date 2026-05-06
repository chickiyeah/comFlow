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

import java.util.List;

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
