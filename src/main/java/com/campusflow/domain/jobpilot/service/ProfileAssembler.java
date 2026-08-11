package com.campusflow.domain.jobpilot.service;

import com.campusflow.domain.award.repository.AwardRepository;
import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 로그인 학생의 실제 데이터(포트폴리오·자격증·인턴·수상·희망직무)를
 * [매칭]·[생성]의 사실 근거인 {@link StudentProfileDto} 로 조립한다.
 *
 * 정직성 가드(§3.1)의 데이터 측 방어선: 여기서 모은 사실만 자소서에 쓰인다.
 * - 포트폴리오 = 개인/학과 프로젝트(isPersonal=true)
 * - 인턴십 = 회사 경력(isPersonal=false)
 * - 자격증/어학 = COMPLETED 만 '취득'으로 인정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileAssembler {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PortfolioRepository portfolioRepository;
    private final CareerActivityRepository careerActivityRepository;
    private final AwardRepository awardRepository;

    public StudentProfileDto assemble(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();

        // 프로젝트 = 포트폴리오
        List<Portfolio> portfolios = portfolioRepository.findByStudentIdOrderByStartDateDesc(sid);
        List<StudentProfileDto.ProjectItem> projects = portfolios.stream()
                .map(p -> new StudentProfileDto.ProjectItem(
                        p.getTitle(),
                        p.getDescription(),
                        splitTech(p.getTechStack()),
                        projectHighlights(p),
                        true   // 포트폴리오는 개인/학과 프로젝트로 간주
                ))
                .toList();

        // 스킬 = 포트폴리오 기술스택 합집합 (순서 보존 dedup)
        Set<String> skillSet = new LinkedHashSet<>();
        portfolios.forEach(p -> skillSet.addAll(splitTech(p.getTechStack())));
        List<String> skills = new ArrayList<>(skillSet);

        // 자격증/어학 = COMPLETED 만 취득으로 인정
        List<String> certifications = new ArrayList<>();
        for (CareerActivity a : careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.CERTIFICATE)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) certifications.add(a.getTitle());
        }
        for (CareerActivity a : careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.LANGUAGE_TEST)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) {
                certifications.add(a.getScore() != null && !a.getScore().isBlank()
                        ? a.getTitle() + " " + a.getScore() : a.getTitle());
            }
        }

        // 회사 경력 = 인턴십
        List<StudentProfileDto.CareerItem> careers = careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.INTERNSHIP).stream()
                .map(a -> new StudentProfileDto.CareerItem(
                        a.getOrganization() == null ? "" : a.getOrganization(),
                        a.getTitle(),
                        period(a),
                        false,                         // 회사 경력
                        a.getMemo() != null && !a.getMemo().isBlank() ? List.of(a.getMemo()) : List.of()
                ))
                .toList();

        // 한 줄 정체성 = 희망직무 + 수상 요약 (사실 기반)
        long awardCount = awardRepository.findByStudentIdOrderByAwardDateDesc(sid).size();
        StringBuilder summary = new StringBuilder();
        if (student.getDesiredJob() != null && !student.getDesiredJob().isBlank()) {
            summary.append(student.getDesiredJob()).append(" 지망");
        }
        if (awardCount > 0) {
            if (summary.length() > 0) summary.append(" · ");
            summary.append("수상 ").append(awardCount).append("건");
        }

        return new StudentProfileDto(
                student.getName(),
                student.getDepartment(),                       // 학력 = 학과 (학교명은 미저장)
                summary.length() > 0 ? summary.toString() : null,
                skills,
                certifications,
                careers,
                projects,
                new ArrayList<>()                              // constraints 는 UI 에서 사용자가 추가
        );
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private static List<String> splitTech(String techStack) {
        if (techStack == null || techStack.isBlank()) return List.of();
        return Arrays.stream(techStack.split("[,/]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static List<String> projectHighlights(Portfolio p) {
        List<String> hs = new ArrayList<>();
        if (p.getRole() != null && !p.getRole().isBlank()) hs.add("역할: " + p.getRole());
        return hs;
    }

    private static String period(CareerActivity a) {
        if (a.getCompletedDate() != null) return a.getCompletedDate().toString();
        if (a.getTargetDate() != null) return a.getTargetDate().toString();
        return null;
    }
}
