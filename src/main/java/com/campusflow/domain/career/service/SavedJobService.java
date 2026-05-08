package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.SavedJobRequest;
import com.campusflow.domain.career.dto.SavedJobResponse;
import com.campusflow.domain.career.entity.SavedJob;
import com.campusflow.domain.career.repository.SavedJobRepository;
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
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<SavedJobResponse> getMine(String username) {
        Student student = getStudent(username);
        return savedJobRepository.findByStudentIdOrderBySavedAtDesc(student.getId())
                .stream().map(SavedJobResponse::from).toList();
    }

    @Transactional
    public SavedJobResponse save(String username, SavedJobRequest req) {
        Student student = getStudent(username);
        SavedJob job = SavedJob.builder()
                .student(student)
                .title(req.title())
                .company(req.company())
                .location(req.location())
                .url(req.url())
                .deadline(req.deadline())
                .jobType(req.jobType())
                .salary(req.salary())
                .description(req.description())
                .source(req.source())
                .build();
        return SavedJobResponse.from(savedJobRepository.save(job));
    }

    @Transactional
    public void delete(String username, Long id) {
        Student student = getStudent(username);
        SavedJob job = savedJobRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        savedJobRepository.delete(job);
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
