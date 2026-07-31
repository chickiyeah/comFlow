package com.campusflow.domain.resume.service;

import com.campusflow.domain.award.entity.Award;
import com.campusflow.domain.award.entity.AwardLevel;
import com.campusflow.domain.award.repository.AwardRepository;
import com.campusflow.domain.career.entity.*;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeAssemblerTest {

    @Mock UserRepository userRepository;
    @Mock StudentRepository studentRepository;
    @Mock PortfolioRepository portfolioRepository;
    @Mock CareerActivityRepository careerActivityRepository;
    @Mock AwardRepository awardRepository;
    @Mock GradeRepository gradeRepository;
    @InjectMocks ResumeAssembler assembler;

    @Test
    void 학생데이터를_이력서_사실섹션으로_조립한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));

        Student student = mock(Student.class);
        when(student.getId()).thenReturn(10L);
        when(student.getName()).thenReturn("홍길동");
        when(student.getStudentId()).thenReturn("201918023");
        when(student.getEmail()).thenReturn("hong@campus.ac");
        when(student.getPhone()).thenReturn("010-1111-2222");
        when(student.getDepartment()).thenReturn("컴퓨터정보과");
        when(student.getGrade()).thenReturn(2);
        when(student.getSemester()).thenReturn(1);
        when(student.getDesiredJob()).thenReturn("백엔드 개발자");
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));

        when(gradeRepository.calculateGpa(10L)).thenReturn(4.05);

        Portfolio p = mock(Portfolio.class);
        when(p.getTitle()).thenReturn("캠퍼스플로우");
        when(p.getDescription()).thenReturn("학과 관리 시스템");
        when(p.getRole()).thenReturn("백엔드");
        when(p.getTechStack()).thenReturn("Java, Spring/React");
        when(p.getStartDate()).thenReturn(java.time.LocalDate.of(2025, 3, 1));
        when(p.getEndDate()).thenReturn(java.time.LocalDate.of(2025, 6, 1));
        when(p.getGithubUrl()).thenReturn("https://github.com/x");
        when(p.getDeployUrl()).thenReturn(null);
        when(portfolioRepository.findByStudentIdOrderByStartDateDesc(10L)).thenReturn(List.of(p));

        CareerActivity cert = mock(CareerActivity.class);
        when(cert.getStatus()).thenReturn(ActivityStatus.COMPLETED);
        when(cert.getTitle()).thenReturn("정보처리기능사");
        when(cert.getOrganization()).thenReturn("한국산업인력공단");
        when(cert.getCompletedDate()).thenReturn(java.time.LocalDate.of(2025, 6, 1));

        CareerActivity certInProgress = mock(CareerActivity.class);
        lenient().when(certInProgress.getStatus()).thenReturn(ActivityStatus.IN_PROGRESS);
        lenient().when(certInProgress.getTitle()).thenReturn("리눅스마스터");

        when(careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(10L, ActivityType.CERTIFICATE))
                .thenReturn(List.of(cert, certInProgress));

        CareerActivity lang = mock(CareerActivity.class);
        when(lang.getStatus()).thenReturn(ActivityStatus.COMPLETED);
        when(lang.getTitle()).thenReturn("TOEIC");
        when(lang.getScore()).thenReturn("800");
        when(lang.getCompletedDate()).thenReturn(java.time.LocalDate.of(2025, 5, 1));

        CareerActivity langInProgress = mock(CareerActivity.class);
        lenient().when(langInProgress.getStatus()).thenReturn(ActivityStatus.IN_PROGRESS);
        lenient().when(langInProgress.getTitle()).thenReturn("JLPT");

        when(careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(10L, ActivityType.LANGUAGE_TEST))
                .thenReturn(List.of(lang, langInProgress));

        CareerActivity intern = mock(CareerActivity.class);
        when(intern.getOrganization()).thenReturn("ABC");
        when(intern.getTitle()).thenReturn("백엔드 인턴");
        when(intern.getMemo()).thenReturn("API 개발");
        when(intern.getCompletedDate()).thenReturn(java.time.LocalDate.of(2025, 8, 1));
        when(careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(10L, ActivityType.INTERNSHIP))
                .thenReturn(List.of(intern));

        Award award = mock(Award.class);
        when(award.getTitle()).thenReturn("교내경진대회");
        when(award.getOrganization()).thenReturn("전주비전대");
        when(award.getLevel()).thenReturn(AwardLevel.GOLD);
        when(award.getAwardDate()).thenReturn(java.time.LocalDate.of(2025, 11, 1));
        when(awardRepository.findByStudentIdOrderByAwardDateDesc(10L)).thenReturn(List.of(award));

        ResumeData data = assembler.assemble("u");

        assertThat(data.personal().name()).isEqualTo("홍길동");
        assertThat(data.education().gpa()).isEqualTo(4.05);
        assertThat(data.skills()).flatExtracting(ResumeData.SkillGroup::items)
                .contains("Java", "Spring", "React");
        assertThat(data.certs()).extracting(ResumeData.CertEntry::name).contains("정보처리기능사").doesNotContain("리눅스마스터");
        assertThat(data.languages()).extracting(ResumeData.LanguageEntry::name).contains("TOEIC").doesNotContain("JLPT");
        assertThat(data.careers()).extracting(ResumeData.CareerEntry::type).containsOnly("경력");
        assertThat(data.awards()).extracting(ResumeData.AwardEntry::level).contains("금상");
        assertThat(data.coverLetter()).isEmpty();
        assertThat(data.targetJob()).isEqualTo("백엔드 개발자");
        assertThat(assembler.buildEvidence(data)).contains("Java").contains("정보처리기능사").contains("희망직무");
    }
}
