package com.campusflow.domain.coverletter.service;

import com.campusflow.domain.coverletter.dto.CoverLetterResponse;
import com.campusflow.domain.coverletter.dto.CoverLetterSaveRequest;
import com.campusflow.domain.coverletter.entity.CoverLetter;
import com.campusflow.domain.coverletter.repository.CoverLetterRepository;
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
public class CoverLetterService {

    private final CoverLetterRepository coverLetterRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<CoverLetterResponse> getMine(String username) {
        Student student = getStudent(username);
        return coverLetterRepository.findByStudentIdOrderByUpdatedAtDesc(student.getId())
                .stream().map(CoverLetterResponse::from).toList();
    }

    @Transactional
    public CoverLetterResponse save(String username, CoverLetterSaveRequest req) {
        Student student = getStudent(username);
        CoverLetter cl = CoverLetter.builder()
                .student(student)
                .title(req.title())
                .companyName(req.companyName())
                .jobTitle(req.jobTitle())
                .content(req.content())
                .build();
        return CoverLetterResponse.from(coverLetterRepository.save(cl));
    }

    @Transactional
    public CoverLetterResponse update(String username, Long id, CoverLetterSaveRequest req) {
        Student student = getStudent(username);
        CoverLetter cl = coverLetterRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        cl.update(req.title(), req.companyName(), req.jobTitle(), req.content());
        return CoverLetterResponse.from(cl);
    }

    @Transactional
    public void delete(String username, Long id) {
        Student student = getStudent(username);
        CoverLetter cl = coverLetterRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        coverLetterRepository.delete(cl);
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
