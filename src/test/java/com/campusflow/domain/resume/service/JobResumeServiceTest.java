package com.campusflow.domain.resume.service;

import com.campusflow.domain.career.entity.SavedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import com.campusflow.domain.career.repository.SavedJobRepository;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import com.campusflow.domain.jobpilot.service.JdExtractorService;
import com.campusflow.domain.jobpilot.service.JobMatcherService;
import com.campusflow.domain.jobpilot.service.ProfileAssembler;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.resume.dto.JobTailoredResumeDraft;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobResumeServiceTest {

    @Mock SavedJobRepository savedJobRepository;
    @Mock ImportedJobRepository importedJobRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;
    @Mock ProfileAssembler profileAssembler;
    @Mock JobMatcherService jobMatcherService;
    @Mock JdExtractorService jdExtractorService;
    @Mock ResumeAiGeneratorService resumeAiGeneratorService;
    @InjectMocks JobResumeService service;

    private ResumeDraft dummyDraft() {
        ResumeData d = new ResumeData(null, null, "백엔드 엔지니어", List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                new ResumeData.Meta("general", null, new HonestyReport(List.of())));
        return new ResumeDraft(d, new HonestyReport(List.of()), "general");
    }

    @Test
    void 저장공고로_생성하면_매칭과_회사직무를_담은_초안을_반환한다() {
        User user = mock(User.class); when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        Student student = mock(Student.class); when(student.getId()).thenReturn(10L);
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));

        SavedJob job = mock(SavedJob.class);
        when(job.getCompany()).thenReturn("네이버");
        when(job.getTitle()).thenReturn("백엔드 엔지니어");
        when(job.getJobType()).thenReturn("정규직");
        when(job.getLocation()).thenReturn("성남");
        when(job.getDeadline()).thenReturn(java.time.LocalDate.of(2026, 9, 1));
        when(job.getSalary()).thenReturn(null);
        when(job.getDescription()).thenReturn("Java와 Spring 기반 백엔드 API를 설계·개발하고 대용량 트래픽을 처리합니다.");
        when(savedJobRepository.findByIdAndStudentId(5L, 10L)).thenReturn(Optional.of(job));

        // description ≥30자 → JD 추출 시도
        JobPosting extracted = new JobPosting("네이버", "백엔드 엔지니어", "정규직", null, null, "성남",
                null, null, List.of("Java", "Spring"),
                List.of(), List.of(), List.of(), List.of(), null, List.of());
        when(jdExtractorService.extract(anyString())).thenReturn(extracted);

        StudentProfileDto profile = new StudentProfileDto("홍길동", "컴퓨터정보과", null,
                List.of("Java"), List.of(), List.of(), List.of(), List.of());
        when(profileAssembler.assemble("u")).thenReturn(profile);

        MatchReport match = new MatchReport(List.of("Java"), List.of("Spring"),
                List.of(), List.of(), "Java 보유");
        when(jobMatcherService.match(any(JobPosting.class), eq(profile))).thenReturn(match);
        when(resumeAiGeneratorService.generateForJob(eq("u"), eq("general"), any(JobPosting.class), eq(match)))
                .thenReturn(dummyDraft());

        JobTailoredResumeDraft result = service.generateForJob("u", "saved", 5L, "general");

        assertThat(result.company()).isEqualTo("네이버");
        assertThat(result.position()).isEqualTo("백엔드 엔지니어");
        assertThat(result.matchReport()).isSameAs(match);
        assertThat(result.draft().template()).isEqualTo("general");

        // 매칭에 넘어간 JobPosting이 추출된 requiredSkills를 담았는지
        ArgumentCaptor<JobPosting> cap = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobMatcherService).match(cap.capture(), eq(profile));
        assertThat(cap.getValue().requiredSkills()).contains("Java", "Spring");
        assertThat(cap.getValue().company()).isEqualTo("네이버");
    }

    @Test
    void 알수없는_jobType은_INVALID_INPUT() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.generateForJob("u", "bogus", 1L, "general"))
                .isInstanceOf(com.campusflow.global.exception.BusinessException.class);
    }
}
