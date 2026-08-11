# 공고 맞춤 이력서 — 플랜 2: 채용목록 → 그 회사 맞춤 이력서 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 사용자가 채용목록(저장공고 SavedJob / 수집공고 ImportedJob)에서 회사를 고르면, 그 공고의 직무·요구스킬에 맞춰 강조점을 조정한 이력서 초안을 자동 생성한다(정직성 검증·매칭 리포트 포함).

**Architecture:** 기존 부품을 잇는 얇은 계층만 추가한다. `JobResumeService`가 선택한 공고를 `JobPosting`으로 변환(SavedJob은 `description`을 `JdExtractorService`로 JD 추출), `JobMatcherService`로 내 프로필과 매칭, `ResumeAiGeneratorService.generateForJob`가 회사·직무·매칭을 프롬프트에 주입해 자소서를 생성(정직성 검증 유지). 결과는 저장하지 않는 초안(`JobTailoredResumeDraft`)으로 반환.

**Tech Stack:** Spring Boot 3.3.5 / Java 17, JUnit5 + Mockito, React 18 + Vite + react-i18next.

## Global Constraints

- `@AuthenticationPrincipal String username` (String, never UserDetails).
- 모든 AI 호출은 `AiFacadeService.ask(...)` 경유(이미 `ResumeAiGeneratorService`·`JdExtractorService` 내부에서 사용). ChromaDB 직접연결 금지.
- 저장 없음: 생성은 초안만 반환. 저장은 기존 `POST /api/resumes`(resumeData/template 포함).
- 정직성: 자소서 사실은 조립된 학생 데이터(evidence)에서만. 공고의 회사·직무·요구스킬은 "지원 대상 정보"로 프롬프트에 주지만, 학생이 보유하지 않은 스킬을 보유한 것처럼 쓰지 않는다(매칭의 gap은 정직하게 다룸).
- 소유권/인증 패턴: `userRepository.findByUsername(username).getId()` → `studentRepository.findByUserId(userId)`; SavedJob은 `findByIdAndStudentId(id, student.getId())`로 소유 검증. ImportedJob은 공용 데이터라 `findById(id)`.
- 추가 기능 곁가지(6양식 등) 금지 — 이 흐름만. 양식은 `general` 하나 사용.
- Maven: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd`. 컴파일 `mvn.cmd -q compile`, 테스트 `mvn.cmd -q test -Dtest=<클래스>`.

## 재사용/기존 시그니처 (탐색 확인값)

- `JobPosting`(record, `domain/jobpilot/dto`): `(String company, String position, String employmentType, String experience, String education, String location, String deadline, String salary, List<String> requiredSkills, List<String> requirements, List<String> preferred, List<String> responsibilities, List<EssayQuestion> essayQuestions, String rawNotes, List<String> missingFields)` + `normalized()`. `deadline`은 **String**.
- `JobMatcherService.match(JobPosting, StudentProfileDto) -> MatchReport`. (`@Service`, 생성자 의존성 없음)
- `MatchReport`(record): `(List<String> matchedSkills, List<String> missingSkills, List<Strength> strengths, List<Gap> gaps, String summary)`; `Strength(String skill, String evidence)`; `Gap(String skill, String severity)`(severity: must|preferred).
- `ProfileAssembler.assemble(String username) -> StudentProfileDto`.
- `JdExtractorService.extract(String jdText) -> JobPosting`(≥30자 필요, 미만이면 `BusinessException(INVALID_INPUT)`; 내부 AI). `requiredSkills()` 접근.
- `SavedJobRepository.findByIdAndStudentId(Long, Long) -> Optional<SavedJob>`; SavedJob getters: `getCompany/getTitle/getJobType/getLocation/getDeadline(LocalDate)/getSalary/getDescription`.
- `ImportedJobRepository.findById(Long) -> Optional<ImportedJob>`(JpaRepository 상속); ImportedJob getters: `getCompany/getTitle/getJobType/getLocation/getDeadline(LocalDate)/getSalary`(description 없음).
- `ResumeAiGeneratorService`(기존): `generate(String username, String template) -> ResumeDraft`, 내부 `generateSection(evidence, targetJob, question)`·`GENERAL_QUESTIONS`·`SYSTEM`/`USER`/`RETRY_*` 상수·`MAX_LEN_RETRY`/`TARGET_LIMIT`/`LIMIT_TYPE`.
- `ResumeAssembler.assemble(username) -> ResumeData`, `buildEvidence(ResumeData) -> String`.
- `ResumeDraft(ResumeData data, HonestyReport honestyReport, String template)`.
- `ErrorCode.NOT_FOUND`, `ErrorCode.INVALID_INPUT`.

## File Structure

**Backend (신규)**
- `domain/resume/dto/JobTailoredResumeDraft.java` — 응답(초안 + 매칭 + 회사/직무).
- `domain/resume/service/JobResumeService.java` — 공고 로드 → JobPosting 변환 → 매칭 → 생성 오케스트레이션.

**Backend (수정)**
- `domain/resume/service/ResumeAiGeneratorService.java` — `generateForJob(...)` 추가 + `generateSection`에 jobContext 파라미터.
- `domain/resume/controller/ResumeController.java` — `POST /generate-for-job`.

**Frontend (수정)**
- `frontend/src/api/resume.js` — `generateResumeForJob({jobType, jobId, template})`.
- `frontend/src/pages/Career.jsx` — 저장공고/수집공고 카드에 "이 회사로 이력서" 버튼 + 맞춤 초안 모달 + 저장.
- `frontend/src/locales/{ko,en,zh,ja,vi}.json` — 신규 키.

**Tests (신규)**
- `src/test/java/com/campusflow/domain/resume/service/ResumeAiGeneratorServiceJobTest.java`
- `src/test/java/com/campusflow/domain/resume/service/JobResumeServiceTest.java`
- `src/test/java/com/campusflow/domain/resume/controller/ResumeGenerateForJobControllerTest.java`

---

### Task 1: ResumeAiGeneratorService.generateForJob — 회사·직무·매칭 주입 생성

**Files:**
- Modify: `src/main/java/com/campusflow/domain/resume/service/ResumeAiGeneratorService.java`
- Test: `src/test/java/com/campusflow/domain/resume/service/ResumeAiGeneratorServiceJobTest.java`

**Interfaces:**
- Consumes: `ResumeAssembler.assemble/buildEvidence`, `HonestyVerifier.verifyAndFix`, `AiFacadeService.ask`, `CharCounter`, `JobPosting`, `MatchReport`.
- Produces: `ResumeDraft generateForJob(String username, String template, JobPosting job, MatchReport match)`. 자소서 4항목을 공고 맥락으로 생성, `ResumeData.targetJob`을 `job.position()`으로 설정.

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.dto.JobPosting;
import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAiGeneratorServiceJobTest {

    @Mock ResumeAssembler assembler;
    @Mock HonestyVerifier honestyVerifier;
    @Mock AiFacadeService ai;
    @InjectMocks ResumeAiGeneratorService service;

    @Test
    void 공고맥락으로_생성하면_targetJob이_공고직무이고_자소서4항목이_나온다() {
        ResumeData facts = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.0),
                "백엔드",                        // 학생 기존 희망직무
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java"))),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), null);
        when(assembler.assemble("u")).thenReturn(facts);
        when(assembler.buildEvidence(facts)).thenReturn("[스킬] Java");
        when(ai.ask(anyString(), anyString())).thenReturn("근거 기반 지원동기 본문입니다.");
        when(honestyVerifier.verifyAndFix(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> new HonestyVerifier.FixResult(inv.getArgument(1), List.of()));

        JobPosting job = new JobPosting("네이버", "백엔드 엔지니어", "정규직", null, null, "성남",
                "2026-09-01", null, List.of("Java", "Spring"),
                List.of(), List.of(), List.of(), List.of(), null, List.of());
        MatchReport match = new MatchReport(
                List.of("Java"), List.of("Spring"),
                List.of(new MatchReport.Strength("Java", "포트폴리오")),
                List.of(new MatchReport.Gap("Spring", "must")), "Java 보유, Spring 미보유");

        ResumeDraft draft = service.generateForJob("u", "general", job, match);

        assertThat(draft.template()).isEqualTo("general");
        assertThat(draft.data().targetJob()).isEqualTo("백엔드 엔지니어");   // 공고 직무로 대체
        assertThat(draft.data().coverLetter()).hasSize(4);
        assertThat(draft.data().meta().honestyReport()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAiGeneratorServiceJobTest`
Expected: FAIL — `generateForJob` 메서드 없음.

- [ ] **Step 3: Write minimal implementation**

`ResumeAiGeneratorService`에 아래를 추가하고, 기존 `generateSection` 호출부를 4-인자 시그니처로 맞춘다.

먼저 기존 `generate(...)`의 루프 안 `generateSection(evidence, targetJob, question)` 호출을 `generateSection(evidence, targetJob, question, null)`로 바꾸고, `generateSection` 시그니처를 확장한다:

```java
    public ResumeDraft generateForJob(String username, String template, JobPosting job, MatchReport match) {
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        ResumeData facts = assembler.assemble(username);
        String evidence = assembler.buildEvidence(facts);
        String targetJob = (job.position() == null || job.position().isBlank())
                ? (facts.targetJob() == null ? "" : facts.targetJob())
                : job.position();
        String jobContext = buildJobContext(job, match);

        List<CoverLetterSection> sections = new ArrayList<>();
        List<HonestyReport.Fix> allFixes = new ArrayList<>();
        for (String question : GENERAL_QUESTIONS) {
            String body = generateSection(evidence, targetJob, question, jobContext);
            HonestyVerifier.FixResult fixed = honestyVerifier.verifyAndFix(question, body, evidence);
            allFixes.addAll(fixed.fixes());
            String finalBody = fixed.text();
            sections.add(new CoverLetterSection(question, finalBody, CharCounter.count(finalBody, true)));
        }

        HonestyReport report = new HonestyReport(allFixes);
        ResumeData withCover = new ResumeData(
                facts.personal(), facts.education(), targetJob, facts.skills(), facts.projects(),
                facts.careers(), facts.certs(), facts.languages(), facts.awards(),
                sections, new Meta(tpl, null, report));
        return new ResumeDraft(withCover, report, tpl);
    }

    /** 공고+매칭을 프롬프트용 맥락 블록으로. 없는 스킬을 보유한 것처럼 쓰지 말라는 정직성은 SYSTEM에서 유지. */
    private String buildJobContext(JobPosting job, MatchReport match) {
        StringBuilder sb = new StringBuilder();
        sb.append("[지원 회사] ").append(orUnknown(job.company())).append('\n');
        sb.append("[지원 직무] ").append(orUnknown(job.position())).append('\n');
        if (job.requiredSkills() != null && !job.requiredSkills().isEmpty()) {
            sb.append("[요구 스킬] ").append(String.join(", ", job.requiredSkills())).append('\n');
        }
        if (match != null) {
            if (match.summary() != null && !match.summary().isBlank()) {
                sb.append("[매칭 요약] ").append(match.summary()).append('\n');
            }
            if (match.strengths() != null && !match.strengths().isEmpty()) {
                sb.append("[부각할 강점] ").append(String.join(", ", match.strengths().stream()
                        .map(s -> s.skill() + "(" + s.evidence() + ")").limit(6).toList())).append('\n');
            }
            if (match.gaps() != null && !match.gaps().isEmpty()) {
                sb.append("[정직하게 다룰 약점] ").append(String.join(", ", match.gaps().stream()
                        .map(g -> g.skill() + "[" + g.severity() + "]").limit(6).toList())).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private static String orUnknown(String s) { return (s == null || s.isBlank()) ? "(미상)" : s; }
```

`generateSection`을 jobContext 파라미터를 받도록 수정한다(기존 본문 유지 + USER 프롬프트에 맥락 주입):

```java
    private String generateSection(String evidence, String targetJob, String question, String jobContext) {
        String tj = (targetJob == null || targetJob.isBlank()) ? "(미지정)" : targetJob;
        LengthVerdict t0 = CharCounter.check("", TARGET_LIMIT, LIMIT_TYPE);
        String jobBlock = (jobContext == null || jobContext.isBlank())
                ? "(특정 공고 없음 — 일반 지원용)"
                : jobContext;
        String user = USER.formatted(evidence, tj, jobBlock, question, t0.targetMin(), t0.targetMax(), TARGET_LIMIT);
        String text = aiFacadeService.ask(SYSTEM, user).trim();

        for (int i = 0; i < MAX_LEN_RETRY; i++) {
            LengthVerdict v = CharCounter.check(text, TARGET_LIMIT, LIMIT_TYPE);
            if (!v.needsRetry()) break;
            String retry = "over".equals(v.status())
                    ? RETRY_OVER.formatted(v.count(), TARGET_LIMIT, v.targetMax(), text)
                    : RETRY_SHORT.formatted(v.count(), v.targetMin(), v.targetMin(), v.targetMax(), text);
            text = aiFacadeService.ask(SYSTEM, retry).trim();
        }
        return text;
    }
```

그리고 `USER` 프롬프트 상수를 맥락 슬롯을 갖도록 교체한다(기존 `[희망 직무]` 다음에 `[지원 맥락]` 추가):

```java
    private static final String USER = """
            [지원자 근거]
            %s

            [희망 직무]
            %s

            [지원 맥락]
            %s

            [작성할 문항]
            "%s"

            [분량]
            - 목표: %d~%d자(공백포함), 최대 %d자.

            위 문항에 대한 자소서 본문만 출력하세요. 지원 맥락이 특정 회사·직무를 가리키면 그에 맞춰 쓰되, 근거에 없는 사실은 만들지 마세요.
            """;
```

> 주의: `USER.formatted(...)` 인자 수가 7개(evidence, tj, jobBlock, question, targetMin, targetMax, TARGET_LIMIT)로 늘었다. 기존 `generate()`의 `generateSection(evidence, targetJob, question, null)` 호출도 이 경로를 타므로 일반 생성은 jobBlock이 "(특정 공고 없음…)"으로 채워진다 — 동작 동일.

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAiGeneratorServiceJobTest,ResumeAiGeneratorServiceTest`
Expected: PASS (신규 + 기존 general 테스트 둘 다 — 기존 테스트가 4-인자 경로로도 통과해야 함).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/service/ResumeAiGeneratorService.java src/test/java/com/campusflow/domain/resume/service/ResumeAiGeneratorServiceJobTest.java
git commit -m "feat(resume): generateForJob — 공고 회사·직무·매칭 주입 자소서 생성"
```

---

### Task 2: JobResumeService — 공고 로드 → JobPosting → 매칭 → 생성

**Files:**
- Create: `src/main/java/com/campusflow/domain/resume/dto/JobTailoredResumeDraft.java`
- Create: `src/main/java/com/campusflow/domain/resume/service/JobResumeService.java`
- Test: `src/test/java/com/campusflow/domain/resume/service/JobResumeServiceTest.java`

**Interfaces:**
- Consumes: `SavedJobRepository.findByIdAndStudentId`, `ImportedJobRepository.findById`, `StudentRepository.findByUserId`, `UserRepository.findByUsername`, `ProfileAssembler.assemble`, `JobMatcherService.match`, `JdExtractorService.extract`, `ResumeAiGeneratorService.generateForJob`.
- Produces:
  - `JobTailoredResumeDraft(ResumeDraft draft, MatchReport matchReport, String company, String position)`.
  - `JobResumeService.generateForJob(String username, String jobType, Long jobId, String template) -> JobTailoredResumeDraft`. jobType: "saved"|"imported" (그 외 INVALID_INPUT).

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=JobResumeServiceTest`
Expected: FAIL — `JobTailoredResumeDraft`/`JobResumeService` 없음.

- [ ] **Step 3: Write minimal implementation**

`JobTailoredResumeDraft.java`:

```java
package com.campusflow.domain.resume.dto;

import com.campusflow.domain.jobpilot.dto.MatchReport;

/** 공고 맞춤 이력서 초안 응답 — 저장 전 프론트로 반환. 매칭 리포트 + 대상 회사/직무 동봉. */
public record JobTailoredResumeDraft(ResumeDraft draft, MatchReport matchReport, String company, String position) {}
```

`JobResumeService.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=JobResumeServiceTest`
Expected: PASS (두 테스트).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/dto/JobTailoredResumeDraft.java src/main/java/com/campusflow/domain/resume/service/JobResumeService.java src/test/java/com/campusflow/domain/resume/service/JobResumeServiceTest.java
git commit -m "feat(resume): JobResumeService — 공고 로드→JobPosting→매칭→맞춤 생성"
```

---

### Task 3: `POST /api/resumes/generate-for-job` 엔드포인트

**Files:**
- Modify: `src/main/java/com/campusflow/domain/resume/controller/ResumeController.java`
- Test: `src/test/java/com/campusflow/domain/resume/controller/ResumeGenerateForJobControllerTest.java`

**Interfaces:**
- Consumes: `JobResumeService.generateForJob(username, jobType, jobId, template)`.
- Produces: `POST /api/resumes/generate-for-job?jobType=&jobId=&template=` → `ApiResponse<JobTailoredResumeDraft>`. `template` 기본 "general".

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.controller;

import com.campusflow.domain.jobpilot.dto.MatchReport;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.JobTailoredResumeDraft;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.resume.service.JobResumeService;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.resume.service.ResumeAiGeneratorService;
import com.campusflow.domain.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class ResumeGenerateForJobControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ResumeService resumeService;
    @MockBean PdfService pdfService;
    @MockBean ResumeAiGeneratorService generatorService;
    @MockBean JobResumeService jobResumeService;

    @Test
    void generateForJob가_맞춤초안을_반환한다() throws Exception {
        ResumeData data = new ResumeData(null, null, "백엔드 엔지니어", List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문", 2)),
                new ResumeData.Meta("general", null, new HonestyReport(List.of())));
        JobTailoredResumeDraft out = new JobTailoredResumeDraft(
                new ResumeDraft(data, new HonestyReport(List.of()), "general"),
                new MatchReport(List.of("Java"), List.of("Spring"), List.of(), List.of(), "요약"),
                "네이버", "백엔드 엔지니어");
        when(jobResumeService.generateForJob(any(), eq("saved"), eq(5L), eq("general"))).thenReturn(out);

        mvc.perform(post("/api/resumes/generate-for-job")
                        .param("jobType", "saved").param("jobId", "5").param("template", "general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company").value("네이버"))
                .andExpect(jsonPath("$.data.position").value("백엔드 엔지니어"))
                .andExpect(jsonPath("$.data.matchReport.matchedSkills[0]").value("Java"))
                .andExpect(jsonPath("$.data.draft.template").value("general"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeGenerateForJobControllerTest`
Expected: FAIL — 매핑/`JobResumeService` 미주입.

- [ ] **Step 3: Write minimal implementation** — `ResumeController`에 필드·엔드포인트 추가:

```java
    private final JobResumeService jobResumeService; // 생성자 주입(@RequiredArgsConstructor)

    @PostMapping("/generate-for-job")
    public ApiResponse<com.campusflow.domain.resume.dto.JobTailoredResumeDraft> generateForJob(
            @AuthenticationPrincipal String username,
            @RequestParam String jobType,
            @RequestParam Long jobId,
            @RequestParam(required = false, defaultValue = "general") String template) {
        return ApiResponse.ok(jobResumeService.generateForJob(username, jobType, jobId, template));
    }
```

`import com.campusflow.domain.resume.service.JobResumeService;` 추가.

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeGenerateForJobControllerTest`
Expected: PASS. 이어서 전체 컴파일: `mvn.cmd -q compile`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/controller/ResumeController.java src/test/java/com/campusflow/domain/resume/controller/ResumeGenerateForJobControllerTest.java
git commit -m "feat(resume): POST /api/resumes/generate-for-job 엔드포인트"
```

---

### Task 4: 프론트 — Career "이 회사로 이력서" 버튼 + 맞춤 초안 모달

**Files:**
- Modify: `frontend/src/api/resume.js`
- Modify: `frontend/src/pages/Career.jsx`
- Modify: `frontend/src/locales/{ko,en,zh,ja,vi}.json`
- Test: 수동 검증(FE 자동 테스트 없음) + `npm run build`.

**Interfaces:**
- Consumes: `POST /api/resumes/generate-for-job?jobType=&jobId=&template=` → axios 인터셉터가 `res.data`(ApiResponse) 반환 → `.data` = `{ draft:{data,honestyReport,template}, matchReport, company, position }`.
- Produces: `generateResumeForJob({ jobType, jobId, template })` in `api/resume.js`.

- [ ] **Step 1: `api/resume.js`에 함수 추가**

```js
// 공고 맞춤 이력서 생성 (초안 반환, 저장 안 함)
export const generateResumeForJob = ({ jobType, jobId, template = 'general' }) =>
  api.post('/resumes/generate-for-job', null, { params: { jobType, jobId, template } })
```

- [ ] **Step 2: `Career.jsx` — 버튼 + 모달 상태 + 핸들러**

READ the file first. Import `generateResumeForJob` from `../api/resume` and `createResume` (if saving from the modal). Add state near other Career state:

```jsx
const [jobResumeDraft, setJobResumeDraft] = useState(null)   // { draft, matchReport, company, position }
const [jobResumeLoading, setJobResumeLoading] = useState(false)
```

Handler — accepts a jobType + numeric id. For saved jobs use `job.id` (numeric). For imported search results the JobSearchResult `id` is like `"imported-<n>"`; parse the trailing number and use jobType `'imported'`. Live (jobkorea/work24) results have no persistent id → for those, prompt the user to save first (skip button).

```jsx
const handleJobResume = async (jobType, jobId) => {
  setJobResumeLoading(true)
  try {
    const res = await generateResumeForJob({ jobType, jobId, template: 'general' })
    setJobResumeDraft(res.data)   // { draft, matchReport, company, position }
  } catch (e) {
    alert(t('career.jobResumeError', '이력서 생성에 실패했습니다. 잠시 후 다시 시도해주세요.'))
  } finally {
    setJobResumeLoading(false)
  }
}

const handleSaveJobResume = async () => {
  if (!jobResumeDraft) return
  const d = jobResumeDraft.draft.data
  const coverText = (d.coverLetter || []).map(s => `[${s.question}]\n${s.body}`).join('\n\n')
  const skillsCsv = (d.skills || []).flatMap(g => g.items || []).join(', ')
  await createResume({
    title: `${jobResumeDraft.company} ${jobResumeDraft.position} 지원 이력서`,
    summary: coverText, skills: skillsCsv, targetJob: d.targetJob || jobResumeDraft.position,
    resumeData: JSON.stringify(d), template: jobResumeDraft.draft.template, portfolioIds: [],
  })
  setJobResumeDraft(null)
  alert(t('career.jobResumeSaved', '이력서로 저장했습니다. 기술 탭에서 편집·PDF 다운로드하세요.'))
}
```

Saved-jobs card (keyed by `job.id`) — add button next to delete:

```jsx
<button type="button" className="btn-secondary text-xs" disabled={jobResumeLoading}
        onClick={() => handleJobResume('saved', job.id)}>
  {jobResumeLoading ? t('career.jobResumeLoading', '생성 중…') : t('career.makeResume', '이 회사로 이력서')}
</button>
```

Imported search-result card (only when the result id starts with `"imported-"`):

```jsx
{typeof job.id === 'string' && job.id.startsWith('imported-') && (
  <button type="button" className="btn-secondary text-xs" disabled={jobResumeLoading}
          onClick={() => handleJobResume('imported', Number(job.id.replace('imported-', '')))}>
    {t('career.makeResume', '이 회사로 이력서')}
  </button>
)}
```

Tailored-draft modal (render when `jobResumeDraft`): shows company·position, match badges (matchedSkills as chips, gaps as warning chips), the 4 cover-letter sections read-only preview, and actions 저장/닫기:

```jsx
{jobResumeDraft && (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setJobResumeDraft(null)}>
    <div className="card max-w-2xl w-full max-h-[85vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
      <h3 className="text-lg font-bold text-primary mb-1">
        {jobResumeDraft.company} · {jobResumeDraft.position}
      </h3>
      <p className="text-sm text-gray-500 mb-3">{jobResumeDraft.matchReport?.summary}</p>
      <div className="flex flex-wrap gap-1 mb-3">
        {(jobResumeDraft.matchReport?.matchedSkills || []).map(s => (
          <span key={s} className="chip-active text-xs">{s}</span>
        ))}
        {(jobResumeDraft.matchReport?.gaps || []).map(g => (
          <span key={g.skill} className="chip text-xs" title={g.severity}>▲ {g.skill}</span>
        ))}
      </div>
      {(jobResumeDraft.draft.data.coverLetter || []).map((s, i) => (
        <div key={i} className="mb-3">
          <div className="font-semibold text-primary text-sm">{s.question}</div>
          <p className="text-sm whitespace-pre-wrap">{s.body}</p>
        </div>
      ))}
      <div className="flex gap-2 justify-end pt-2">
        <button className="btn-secondary" onClick={() => setJobResumeDraft(null)}>{t('career.close', '닫기')}</button>
        <button className="btn-primary" onClick={handleSaveJobResume}>{t('career.saveResume', '이력서로 저장')}</button>
      </div>
    </div>
  </div>
)}
```

> Adapt class names / structure to Career.jsx's actual patterns (verify `.card`, `.chip`, `.chip-active`, `.btn-primary`, `.btn-secondary` exist — they are in the design system). If Career.jsx already imports `createResume`, reuse; else import from `../api/resume`.

- [ ] **Step 3: i18n 키 (5개 언어)** — 각 언어 파일의 적절한 네임스페이스(`career.*`)에 추가:

```
career.makeResume       : "이 회사로 이력서" / "Resume for this job" / "为该公司生成简历" / "この会社向け履歴書" / "Hồ sơ cho công ty này"
career.jobResumeLoading : "생성 중…"
career.saveResume       : "이력서로 저장"
career.close            : "닫기"
career.jobResumeError   : "이력서 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
career.jobResumeSaved   : "이력서로 저장했습니다. 기술 탭에서 편집·PDF 다운로드하세요."
```
> 실제 파일 구조(`career` 최상위 객체)에 맞춰 추가하고, 이미 존재하는 키는 재사용한다. 참조하는 모든 신규 `career.*` 키가 5개 언어 전부에 있어야 한다(누락 시 리터럴 노출).

- [ ] **Step 4: 빌드 + 수동 검증**

Run: `cd frontend && npm run build` — 성공해야 함.
수동 검증(로컬 8080+3000): Career → 저장공고/수집공고 카드의 "이 회사로 이력서" → 모달에 회사·직무·매칭 배지·자소서 4항목 표시 → "이력서로 저장" → 기술 탭 이력서 목록·PDF 확인.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/resume.js frontend/src/pages/Career.jsx frontend/src/locales/ko.json frontend/src/locales/en.json frontend/src/locales/zh.json frontend/src/locales/ja.json frontend/src/locales/vi.json
git commit -m "feat(resume): Career 공고 카드 '이 회사로 이력서' + 맞춤 초안 모달 + i18n"
```

---

## Self-Review (작성자 체크)

**Spec coverage:** 공고→JobPosting+매칭(Task2) / 맞춤 생성(Task1) / 엔드포인트(Task3) / 프론트 버튼·모달·저장(Task4). 목표("채용목록에서 회사 선택 → 맞춤 이력서") 전 구간 커버.

**Placeholder scan:** 없음. 모든 코드 스텝에 실제 코드.

**Type consistency:**
- `JobPosting` 15-인자 생성자 순서(company, position, employmentType, experience, education, location, deadline(String), salary, requiredSkills, requirements, preferred, responsibilities, essayQuestions, rawNotes, missingFields) — Task1 테스트·Task2 `toPosting` 동일.
- `MatchReport(matchedSkills, missingSkills, strengths[Strength(skill,evidence)], gaps[Gap(skill,severity)], summary)` — 테스트·프롬프트 동일.
- `ResumeDraft(data, honestyReport, template)` / `ResumeData` 11-인자(targetJob 3번째 포함) — 기존과 일치.
- `generateForJob(username, template, JobPosting, MatchReport)` — Task1 정의, Task2 호출 일치.
- `JobResumeService.generateForJob(username, jobType, jobId, template)` — Task2 정의, Task3 호출 일치.

**주의:** Task1은 기존 `generateSection` 시그니처를 4-인자로 바꾸고 `USER` 프롬프트 상수를 7-슬롯으로 교체하므로, 기존 `generate()`의 호출부도 반드시 함께 수정해야 컴파일된다(플랜에 명시). 프론트 라이브(비수집) 검색 결과는 영속 id가 없어 버튼을 노출하지 않는다(저장 후 생성) — 의도된 제약.
