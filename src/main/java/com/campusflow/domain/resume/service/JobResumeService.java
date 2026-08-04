package com.campusflow.domain.resume.service;

import com.campusflow.domain.career.entity.ImportedJob;
import com.campusflow.domain.career.entity.SavedJob;
import com.campusflow.domain.career.repository.ImportedJobRepository;
import com.campusflow.domain.career.repository.SavedJobRepository;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.jobpilot.dto.StudentProfileDto;
import com.campusflow.domain.jobpilot.service.JdExtractorService;
import com.campusflow.domain.jobpilot.service.JobMatcherService;
import com.campusflow.domain.jobpilot.service.ProfileAssembler;
import com.campusflow.domain.resume.dto.JobTailoredResumeDraft;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 채용목록(저장공고/수집공고)에서 고른 공고에 맞춘 이력서 초안 생성.
 * 공고 → JobPosting 변환(SavedJob description은 JD 추출) → 프로필 매칭 → generateForJob.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobResumeService {

    private final SavedJobRepository savedJobRepository;
    private final ImportedJobRepository importedJobRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ProfileAssembler profileAssembler;
    private final JobMatcherService jobMatcherService;
    private final JdExtractorService jdExtractorService;
    private final ResumeAiGeneratorService resumeAiGeneratorService;

    public JobTailoredResumeDraft generateForJob(String username, String jobType, Long jobId, String template) {
        JobPosting job = loadJobPosting(username, jobType, jobId);
        StudentProfileDto profile = profileAssembler.assemble(username);
        MatchReport match = jobMatcherService.match(job, profile);
        ResumeDraft draft = resumeAiGeneratorService.generateForJob(username, template, job, match);
        return new JobTailoredResumeDraft(draft, match, job.company(), job.position());
    }

    private JobPosting loadJobPosting(String username, String jobType, Long jobId) {
        if ("saved".equalsIgnoreCase(jobType)) {
            Student student = getStudent(username);
            SavedJob j = savedJobRepository.findByIdAndStudentId(jobId, student.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            return toPosting(j.getCompany(), j.getTitle(), j.getJobType(),
                    j.getLocation(), j.getDeadline(), j.getSalary(), j.getDescription());
        }
        if ("imported".equalsIgnoreCase(jobType)) {
            ImportedJob j = importedJobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            return toPosting(j.getCompany(), j.getTitle(), j.getJobType(),
                    j.getLocation(), j.getDeadline(), j.getSalary(), null);
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private JobPosting toPosting(String company, String title, String jobType,
                                 String location, LocalDate deadline, String salary, String description) {
        List<String> required = List.of();
        if (description != null && description.trim().length() >= 30) {
            try {
                List<String> ex = jdExtractorService.extract(description).requiredSkills();
                if (ex != null) required = ex;
            } catch (Exception e) {
                log.warn("[JobResume] JD 추출 실패 — 요구스킬 없이 진행: {}", e.getMessage());
            }
        }
        return new JobPosting(
                company, title, jobType, null, null, location,
                deadline == null ? null : deadline.toString(), salary,
                required, List.of(), List.of(), List.of(), List.of(), null, List.of());
    }

    private Student getStudent(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
                .getId();
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
    }
}
