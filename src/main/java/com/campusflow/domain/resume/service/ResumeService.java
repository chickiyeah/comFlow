package com.campusflow.domain.resume.service;

import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.resume.dto.ResumeRequest;
import com.campusflow.domain.resume.dto.ResumeResponse;
import com.campusflow.domain.resume.entity.Resume;
import com.campusflow.domain.resume.entity.ResumePortfolio;
import com.campusflow.domain.resume.repository.ResumeRepository;
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
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final PortfolioRepository portfolioRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<ResumeResponse> getMyResumes(String username) {
        Student student = getStudentByUsername(username);
        return resumeRepository.findByStudentIdOrderByUpdatedAtDesc(student.getId())
                .stream().map(ResumeResponse::from).toList();
    }

    public ResumeResponse getResume(String username, Long resumeId) {
        Student student = getStudentByUsername(username);
        return ResumeResponse.from(findResume(resumeId, student.getId()));
    }

    @Transactional
    public ResumeResponse create(String username, ResumeRequest request) {
        Student student = getStudentByUsername(username);
        Resume resume = Resume.builder()
                .student(student)
                .title(request.title())
                .summary(request.summary())
                .skills(request.skills())
                .targetJob(request.targetJob())
                .build();
        resumeRepository.save(resume);
        linkPortfolios(resume, student.getId(), request.portfolioIds());
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse update(String username, Long resumeId, ResumeRequest request) {
        Student student = getStudentByUsername(username);
        Resume resume = findResume(resumeId, student.getId());
        resume.update(request.title(), request.summary(), request.skills(), request.targetJob());
        resume.clearPortfolios();
        linkPortfolios(resume, student.getId(), request.portfolioIds());
        return ResumeResponse.from(resume);
    }

    @Transactional
    public void delete(String username, Long resumeId) {
        Student student = getStudentByUsername(username);
        resumeRepository.delete(findResume(resumeId, student.getId()));
    }

    private void linkPortfolios(Resume resume, Long studentId, List<Long> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) return;
        for (int i = 0; i < portfolioIds.size(); i++) {
            Long pid = portfolioIds.get(i);
            Portfolio portfolio = portfolioRepository.findByIdAndStudentId(pid, studentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            resume.getResumePortfolios().add(new ResumePortfolio(resume, portfolio, i));
        }
    }

    private Resume findResume(Long resumeId, Long studentId) {
        return resumeRepository.findByIdAndStudentId(resumeId, studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
