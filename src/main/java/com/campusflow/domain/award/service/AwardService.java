package com.campusflow.domain.award.service;

import com.campusflow.domain.award.dto.AwardRequest;
import com.campusflow.domain.award.dto.AwardResponse;
import com.campusflow.domain.award.entity.Award;
import com.campusflow.domain.award.repository.AwardRepository;
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
public class AwardService {

    private final AwardRepository awardRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public List<AwardResponse> getMyAwards(String username) {
        Student student = getStudentByUsername(username);
        return awardRepository.findByStudentIdOrderByAwardDateDesc(student.getId())
                .stream().map(AwardResponse::from).toList();
    }

    @Transactional
    public AwardResponse create(String username, AwardRequest request) {
        Student student = getStudentByUsername(username);
        Award award = Award.builder()
                .student(student)
                .title(request.title())
                .organization(request.organization())
                .level(request.level())
                .awardDate(request.awardDate())
                .description(request.description())
                .build();
        return AwardResponse.from(awardRepository.save(award));
    }

    @Transactional
    public void delete(String username, Long id) {
        Student student = getStudentByUsername(username);
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!award.getStudent().getId().equals(student.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        awardRepository.delete(award);
    }

    private Student getStudentByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
