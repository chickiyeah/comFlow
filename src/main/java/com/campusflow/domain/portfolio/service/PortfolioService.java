package com.campusflow.domain.portfolio.service;

import com.campusflow.domain.portfolio.dto.PortfolioRequest;
import com.campusflow.domain.portfolio.dto.PortfolioResponse;
import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
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
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<PortfolioResponse> getMyPortfolios(String username) {
        Student student = getStudentByUsername(username);
        return portfolioRepository.findByStudentIdOrderByStartDateDesc(student.getId())
                .stream().map(PortfolioResponse::from).toList();
    }

    @Transactional
    public PortfolioResponse create(String username, PortfolioRequest request) {
        Student student = getStudentByUsername(username);
        Portfolio portfolio = Portfolio.builder()
                .student(student)
                .title(request.title())
                .description(request.description())
                .role(request.role())
                .techStack(request.techStack())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .githubUrl(request.githubUrl())
                .deployUrl(request.deployUrl())
                .status(request.status())
                .build();
        return PortfolioResponse.from(portfolioRepository.save(portfolio));
    }

    @Transactional
    public PortfolioResponse update(String username, Long portfolioId, PortfolioRequest request) {
        Student student = getStudentByUsername(username);
        Portfolio portfolio = portfolioRepository.findByIdAndStudentId(portfolioId, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        portfolio.update(request.title(), request.description(), request.role(), request.techStack(),
                request.startDate(), request.endDate(), request.githubUrl(), request.deployUrl(), request.status());
        return PortfolioResponse.from(portfolio);
    }

    @Transactional
    public void delete(String username, Long portfolioId) {
        Student student = getStudentByUsername(username);
        Portfolio portfolio = portfolioRepository.findByIdAndStudentId(portfolioId, student.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        portfolioRepository.delete(portfolio);
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
