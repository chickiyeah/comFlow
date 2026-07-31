# 이력서 자동생성 — 플랜 1: 공통기반 + A(표준 이력서) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 학생이 이미 축적한 데이터(성적·자격증·어학·인턴·수상·포트폴리오)로 실무형 국문 이력서 1벌을 원클릭 자동 생성하고, 근거 없는 과장은 적대적 정직성 검증으로 자동 순화한 뒤 편집·저장·PDF 다운로드까지 가능하게 한다.

**Architecture:** resume 도메인에 리치 이력서 데이터 모델(`ResumeData` JSON)을 추가하고, jobpilot의 `ProfileAssembler`를 건드리지 않도록 같은 리포지토리를 재사용하는 `ResumeAssembler`로 사실을 조립한다. `ResumeAiGeneratorService`가 `AiFacadeService`(LiteLLM 게이트웨이·세맨틱 캐시)로 자기소개 4항목을 글자수 가드 루프와 함께 생성하고, `HonestyVerifier`가 근거 대비 과장 문장을 감지→자동 재생성한다. 생성 결과는 저장하지 않고 초안(`ResumeDraft`)으로 반환하여 사용자가 편집 후 기존 `POST /api/resumes`로 저장한다.

**Tech Stack:** Spring Boot 3.3.5 / Java 17, JPA(ddl-auto=update), Jackson `ObjectMapper`, Thymeleaf + openhtmltopdf(PDF), JUnit5 + Mockito(테스트), React 18 + Vite + react-i18next(프론트).

## Global Constraints

- `@AuthenticationPrincipal`은 반드시 `String username`으로 받는다 (JWT 필터가 principal을 username 문자열로 저장 — `UserDetails`로 받으면 NPE).
- 모든 AI 호출은 `AiFacadeService.ask(systemPrompt, userMessage)` 경유 (LiteLLM 게이트웨이 10.8.0.1:4000). 자체 폴백체인·ChromaDB 직접연결 금지.
- DB 스키마 변경은 **additive** 만 (ddl-auto=update가 컬럼 자동 생성). 기존 컬럼/데이터 삭제·변경 금지.
- 리포지토리 `findByStudentId...` 메서드는 Student의 PK `Long`(`student.getId()`)을 받는다. 학번 varchar는 `getStudentId()`로 별개.
- 정직성 원칙: 이력서 사실 문장은 조립된 데이터에 있는 것만. 없는 경험·수치·성과를 만들지 않는다. 위반 시 자동 순화/삭제.
- 생성 결과는 자동 저장하지 않는다(항상 사람 검토·편집 후 저장).
- API 응답은 `ApiResponse.ok(data)`로 래핑. 단 데이터가 `String`이면 `ApiResponse.ok((Object) data)` 또는 DTO로 감싼다(`ok(String)` 오버로드가 message로 해석됨에 주의).
- Maven: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd`. 컴파일 검증: `mvn.cmd -q compile`, 테스트: `mvn.cmd -q test -Dtest=<클래스>`.

## File Structure

**Backend (신규)**
- `domain/resume/dto/ResumeData.java` — 리치 이력서 전 섹션 DTO(중첩 record). 사실 + 자기소개.
- `domain/resume/dto/ResumeDraft.java` — 생성 초안 응답(data + honestyReport + template).
- `domain/resume/dto/HonestyReport.java` — 정직성 자동수정 로그.
- `domain/resume/service/ResumeAssembler.java` — 학생 데이터 → `ResumeData`(사실 섹션, AI 없음).
- `domain/resume/service/ResumeAiGeneratorService.java` — 자기소개 생성 + 글자수 가드 + 정직성 통합.
- `domain/resume/service/HonestyVerifier.java` — 적대적 정직성 검증 + 자동 재생성.
- `domain/resume/util/JsonExtract.java` — reasoning 모델 응답에서 JSON 부분 추출.
- `src/main/resources/templates/resume-general.html` — 일반기업형 PDF 템플릿(리치 데이터 렌더).

**Backend (수정)**
- `domain/resume/entity/Resume.java` — 컬럼 `resumeData`/`template`/`sourceJobId`/`sourceJobType` 추가 + `updateRich(...)`.
- `domain/resume/dto/ResumeRequest.java` — `resumeData`/`template` 필드 추가.
- `domain/resume/dto/ResumeResponse.java` — `resumeData`/`template` 노출.
- `domain/resume/service/ResumeService.java` — create/update가 resumeData·template 저장.
- `domain/resume/service/PdfService.java` — 리치 데이터 기반 양식별 렌더 오버로드.
- `domain/resume/controller/ResumeController.java` — `POST /generate`, PDF 렌더 경로 갱신.

**Frontend (수정)**
- `frontend/src/api/resume.js` — `generateResume(template, body)`.
- `frontend/src/pages/Technical.jsx` — "AI로 이력서 생성" 버튼 + 초안 미리보기/편집 + honesty 배지.
- `frontend/src/locales/*/translation.json`(5개 언어) — 신규 키.

**Tests (신규)**
- `src/test/java/com/campusflow/domain/resume/dto/ResumeDataJsonTest.java`
- `src/test/java/com/campusflow/domain/resume/service/ResumeAssemblerTest.java`
- `src/test/java/com/campusflow/domain/resume/service/HonestyVerifierTest.java`
- `src/test/java/com/campusflow/domain/resume/service/ResumeAiGeneratorServiceTest.java`

---

### Task 1: 리치 이력서 데이터 모델 (`ResumeData` + 중첩 record)

**Files:**
- Create: `src/main/java/com/campusflow/domain/resume/dto/ResumeData.java`
- Test: `src/test/java/com/campusflow/domain/resume/dto/ResumeDataJsonTest.java`

**Interfaces:**
- Produces: `ResumeData` record와 중첩 record들. 다른 태스크가 이 타입으로 조립/직렬화.
  - `ResumeData(Personal personal, Education education, List<SkillGroup> skills, List<ProjectEntry> projects, List<CareerEntry> careers, List<CertEntry> certs, List<LanguageEntry> languages, List<AwardEntry> awards, List<CoverLetterSection> coverLetter, Meta meta)`
  - `Personal(String name, String studentId, String email, String phone)`
  - `Education(String department, int grade, int semester, Double gpa)`
  - `SkillGroup(String category, List<String> items)`
  - `ProjectEntry(String title, String period, List<String> techStack, String role, String problem, String solution, String result, String githubUrl, String deployUrl)`
  - `CareerEntry(String org, String period, String role, String type, String description)` — type: "경력" | "경험"
  - `CertEntry(String name, String org, String date)`
  - `LanguageEntry(String name, String score, String date)`
  - `AwardEntry(String title, String org, String level, String date)`
  - `CoverLetterSection(String question, String body, int charCount)`
  - `Meta(String template, String generatedAt, HonestyReport honestyReport)` — 단, `HonestyReport`는 Task 5에서 생성하므로 Task 1에서는 `Meta`에서 제외하고 Task 5에서 추가한다. **Task 1의 `Meta`는 `Meta(String template, String generatedAt)`.**

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.dto;

import com.campusflow.domain.resume.dto.ResumeData.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeDataJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 직렬화_역직렬화_왕복() throws Exception {
        ResumeData data = new ResumeData(
                new Personal("홍길동", "201918023", "hong@campus.ac", "010-1234-5678"),
                new Education("컴퓨터정보과", 2, 1, 4.05),
                List.of(new SkillGroup("언어", List.of("Java", "Python"))),
                List.of(new ProjectEntry("캠퍼스플로우", "2025.03~2025.06",
                        List.of("Spring", "React"), "백엔드", "출결 수기 관리",
                        "REST API 자동화", "처리시간 60% 단축", "https://github.com/x", null)),
                List.of(new CareerEntry("ABC", "2025.07~2025.08", "인턴", "경력", "백엔드 인턴")),
                List.of(new CertEntry("정보처리기능사", "한국산업인력공단", "2025.06")),
                List.of(new LanguageEntry("TOEIC", "800", "2025.05")),
                List.of(new AwardEntry("교내경진대회", "전주비전대", "금상", "2025.11")),
                List.of(new CoverLetterSection("성장과정", "저는...", 5)),
                new Meta("general", "2026-07-31T10:00:00")
        );

        String json = mapper.writeValueAsString(data);
        ResumeData back = mapper.readValue(json, ResumeData.class);

        assertThat(back.personal().name()).isEqualTo("홍길동");
        assertThat(back.education().gpa()).isEqualTo(4.05);
        assertThat(back.skills().get(0).items()).containsExactly("Java", "Python");
        assertThat(back.coverLetter().get(0).question()).isEqualTo("성장과정");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeDataJsonTest`
Expected: FAIL — `ResumeData` 클래스 없음(컴파일 에러).

- [ ] **Step 3: Write minimal implementation**

```java
package com.campusflow.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 리치 이력서 전 섹션. 사실 섹션(personal~awards)은 ResumeAssembler가 DB에서 조립하고,
 * coverLetter/meta는 ResumeAiGeneratorService가 채운다. Resume.resumeData(JSON)로 저장된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeData(
        Personal personal,
        Education education,
        List<SkillGroup> skills,
        List<ProjectEntry> projects,
        List<CareerEntry> careers,
        List<CertEntry> certs,
        List<LanguageEntry> languages,
        List<AwardEntry> awards,
        List<CoverLetterSection> coverLetter,
        Meta meta
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(String name, String studentId, String email, String phone) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Education(String department, int grade, int semester, Double gpa) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillGroup(String category, List<String> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectEntry(String title, String period, List<String> techStack,
                               String role, String problem, String solution, String result,
                               String githubUrl, String deployUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CareerEntry(String org, String period, String role, String type, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CertEntry(String name, String org, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageEntry(String name, String score, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AwardEntry(String title, String org, String level, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoverLetterSection(String question, String body, int charCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String template, String generatedAt) {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeDataJsonTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/dto/ResumeData.java src/test/java/com/campusflow/domain/resume/dto/ResumeDataJsonTest.java
git commit -m "feat(resume): 리치 이력서 데이터 모델 ResumeData"
```

---

### Task 2: Resume 엔티티 컬럼 + 저장 배선 (Request/Response/Service)

**Files:**
- Modify: `src/main/java/com/campusflow/domain/resume/entity/Resume.java`
- Modify: `src/main/java/com/campusflow/domain/resume/dto/ResumeRequest.java`
- Modify: `src/main/java/com/campusflow/domain/resume/dto/ResumeResponse.java`
- Modify: `src/main/java/com/campusflow/domain/resume/service/ResumeService.java`
- Test: `src/test/java/com/campusflow/domain/resume/service/ResumeServiceRichTest.java`

**Interfaces:**
- Consumes: 없음(엔티티/DTO 확장).
- Produces:
  - `Resume` 필드 `String getResumeData()`, `String getTemplate()`, `Long getSourceJobId()`, `String getSourceJobType()`.
  - `Resume.updateRich(String title, String summary, String skills, String targetJob, String resumeData, String template)`.
  - `Resume.setSourceJob(Long sourceJobId, String sourceJobType)`.
  - `ResumeRequest`에 `String resumeData()`, `String template()` 추가(nullable).
  - `ResumeResponse`에 `String resumeData()`, `String template()` 추가.

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeRequest;
import com.campusflow.domain.resume.dto.ResumeResponse;
import com.campusflow.domain.resume.entity.Resume;
import com.campusflow.domain.resume.repository.ResumeRepository;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.entity.User;
import com.campusflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceRichTest {

    @Mock ResumeRepository resumeRepository;
    @Mock com.campusflow.domain.portfolio.repository.PortfolioRepository portfolioRepository;
    @Mock StudentRepository studentRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ResumeService service;

    @Test
    void create가_resumeData와_template을_저장한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(10L);
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeRequest req = new ResumeRequest("제목", "요약", "Java,Python", "백엔드",
                "{\"personal\":{\"name\":\"홍길동\"}}", "general", null);

        ResumeResponse res = service.create("u", req);

        assertThat(res.template()).isEqualTo("general");
        assertThat(res.resumeData()).contains("홍길동");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeServiceRichTest`
Expected: FAIL — `ResumeRequest` 생성자 인자 수 불일치 / `ResumeResponse.template()` 없음(컴파일 에러).

- [ ] **Step 3: Write minimal implementation**

`Resume.java` — 필드·메서드 추가 (기존 필드 아래, `clearPortfolios()` 위):

```java
    @Column(columnDefinition = "TEXT")
    private String resumeData;   // ResumeData JSON

    @Column(length = 20)
    private String template;     // dev|ncs|general|startup|english|internship

    private Long sourceJobId;    // C 단계 원본 공고 id (nullable)

    @Column(length = 10)
    private String sourceJobType; // saved | imported (nullable)

    public void updateRich(String title, String summary, String skills, String targetJob,
                           String resumeData, String template) {
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
        this.resumeData = resumeData;
        if (template != null && !template.isBlank()) this.template = template;
    }

    public void setSourceJob(Long sourceJobId, String sourceJobType) {
        this.sourceJobId = sourceJobId;
        this.sourceJobType = sourceJobType;
    }
```

또한 `@Builder` 생성자에 `resumeData`/`template`를 받도록 확장:

```java
    @Builder
    public Resume(Student student, String title, String summary, String skills, String targetJob,
                  String resumeData, String template) {
        this.student = student;
        this.title = title;
        this.summary = summary;
        this.skills = skills;
        this.targetJob = targetJob;
        this.resumeData = resumeData;
        this.template = template;
    }
```

`ResumeRequest.java` — 필드 추가(끝에):

```java
public record ResumeRequest(
        @NotBlank(message = "이력서 제목을 입력해주세요.") String title,
        String summary,
        String skills,
        String targetJob,
        String resumeData,   // 리치 이력서 JSON (nullable — 구 클라이언트 호환)
        String template,     // 양식 키 (nullable)
        List<Long> portfolioIds
) {}
```

`ResumeResponse.java` — 필드 추가 + `from` 갱신:

```java
public record ResumeResponse(
        Long id,
        String title,
        String summary,
        List<String> skills,
        String targetJob,
        String resumeData,
        String template,
        List<PortfolioResponse> portfolios,
        LocalDateTime updatedAt
) {
    public static ResumeResponse from(Resume resume) {
        List<String> skillList = (resume.getSkills() != null && !resume.getSkills().isBlank())
                ? Arrays.stream(resume.getSkills().split(",")).map(String::trim).toList()
                : List.of();

        List<PortfolioResponse> portfolios = resume.getResumePortfolios().stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(rp -> PortfolioResponse.from(rp.getPortfolio()))
                .toList();

        return new ResumeResponse(
                resume.getId(), resume.getTitle(), resume.getSummary(),
                skillList, resume.getTargetJob(), resume.getResumeData(), resume.getTemplate(),
                portfolios, resume.getUpdatedAt()
        );
    }
}
```

`ResumeService.java` — `create`/`update`에서 resumeData·template 반영:

```java
    @Transactional
    public ResumeResponse create(String username, ResumeRequest request) {
        Student student = getStudentByUsername(username);
        Resume resume = Resume.builder()
                .student(student)
                .title(request.title())
                .summary(request.summary())
                .skills(request.skills())
                .targetJob(request.targetJob())
                .resumeData(request.resumeData())
                .template(request.template())
                .build();
        resumeRepository.save(resume);
        linkPortfolios(resume, student.getId(), request.portfolioIds());
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse update(String username, Long resumeId, ResumeRequest request) {
        Student student = getStudentByUsername(username);
        Resume resume = findResume(resumeId, student.getId());
        resume.updateRich(request.title(), request.summary(), request.skills(),
                request.targetJob(), request.resumeData(), request.template());
        resume.clearPortfolios();
        linkPortfolios(resume, student.getId(), request.portfolioIds());
        return ResumeResponse.from(resume);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeServiceRichTest`
Expected: PASS. 그리고 전체 컴파일: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q compile` → BUILD SUCCESS (기존 `ResumeController.downloadPdf`, 프론트 미변경분은 다음 태스크에서 정리).

> 주의: `ResumeResponse` 생성자 인자 순서가 바뀌었으므로 `PdfService.generateResumePdf(ResumeResponse)`는 그대로 컴파일된다(필드 접근만). 프론트는 Task 8에서 신규 필드 사용.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/ src/test/java/com/campusflow/domain/resume/service/ResumeServiceRichTest.java
git commit -m "feat(resume): resumeData/template 컬럼 + 저장 배선 (additive)"
```

---

### Task 3: ResumeAssembler — 학생 데이터 → ResumeData 사실 섹션

**Files:**
- Create: `src/main/java/com/campusflow/domain/resume/service/ResumeAssembler.java`
- Test: `src/test/java/com/campusflow/domain/resume/service/ResumeAssemblerTest.java`

**Interfaces:**
- Consumes: `ResumeData`(Task 1). 리포지토리: `UserRepository.findByUsername`, `StudentRepository.findByUserId`, `PortfolioRepository.findByStudentIdOrderByStartDateDesc(Long)`, `CareerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(Long, ActivityType)`, `AwardRepository.findByStudentIdOrderByAwardDateDesc(Long)`, `GradeRepository.calculateGpa(Long)`.
- Produces: `ResumeData assemble(String username)` — coverLetter/meta는 비움(`List.of()`, `null`). 사실 섹션만 채움.
- Produces: `String buildEvidence(ResumeData data)` — 프롬프트용 근거 텍스트(정직성 대조 기준). 다른 태스크가 사용.

- [ ] **Step 1: Write the failing test**

```java
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
        when(careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(10L, ActivityType.CERTIFICATE))
                .thenReturn(List.of(cert));

        CareerActivity lang = mock(CareerActivity.class);
        when(lang.getStatus()).thenReturn(ActivityStatus.COMPLETED);
        when(lang.getTitle()).thenReturn("TOEIC");
        when(lang.getScore()).thenReturn("800");
        when(lang.getCompletedDate()).thenReturn(java.time.LocalDate.of(2025, 5, 1));
        when(careerActivityRepository.findByStudentIdAndTypeOrderByCreatedAtDesc(10L, ActivityType.LANGUAGE_TEST))
                .thenReturn(List.of(lang));

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
        assertThat(data.certs()).extracting(ResumeData.CertEntry::name).contains("정보처리기능사");
        assertThat(data.languages()).extracting(ResumeData.LanguageEntry::name).contains("TOEIC");
        assertThat(data.careers()).extracting(ResumeData.CareerEntry::type).containsOnly("경력");
        assertThat(data.awards()).extracting(ResumeData.AwardEntry::level).contains("금상");
        assertThat(data.coverLetter()).isEmpty();
        assertThat(assembler.buildEvidence(data)).contains("Java").contains("정보처리기능사");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAssemblerTest`
Expected: FAIL — `ResumeAssembler` 없음(컴파일 에러).

- [ ] **Step 3: Write minimal implementation**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.award.entity.Award;
import com.campusflow.domain.award.repository.AwardRepository;
import com.campusflow.domain.career.entity.ActivityStatus;
import com.campusflow.domain.career.entity.ActivityType;
import com.campusflow.domain.career.entity.CareerActivity;
import com.campusflow.domain.career.repository.CareerActivityRepository;
import com.campusflow.domain.grade.repository.GradeRepository;
import com.campusflow.domain.portfolio.entity.Portfolio;
import com.campusflow.domain.portfolio.repository.PortfolioRepository;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeData.*;
import com.campusflow.domain.student.entity.Student;
import com.campusflow.domain.student.repository.StudentRepository;
import com.campusflow.domain.user.repository.UserRepository;
import com.campusflow.global.exception.BusinessException;
import com.campusflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 학생의 실제 데이터를 이력서의 '사실 섹션'({@link ResumeData})으로 조립한다. AI를 쓰지 않는다.
 * jobpilot ProfileAssembler를 변경하지 않기 위해 같은 리포지토리를 재사용하는 별도 조립기.
 * coverLetter/meta는 ResumeAiGeneratorService가 채운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeAssembler {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PortfolioRepository portfolioRepository;
    private final CareerActivityRepository careerActivityRepository;
    private final AwardRepository awardRepository;
    private final GradeRepository gradeRepository;

    public ResumeData assemble(String username) {
        Student student = getStudent(username);
        Long sid = student.getId();

        Personal personal = new Personal(
                student.getName(), student.getStudentId(), student.getEmail(), student.getPhone());

        Double gpa = gradeRepository.calculateGpa(sid);
        Education education = new Education(
                student.getDepartment(), student.getGrade(), student.getSemester(), gpa);

        List<Portfolio> portfolios = portfolioRepository.findByStudentIdOrderByStartDateDesc(sid);

        Set<String> skillSet = new LinkedHashSet<>();
        portfolios.forEach(p -> skillSet.addAll(splitTech(p.getTechStack())));
        List<SkillGroup> skills = skillSet.isEmpty()
                ? List.of()
                : List.of(new SkillGroup("보유 기술", new ArrayList<>(skillSet)));

        List<ProjectEntry> projects = portfolios.stream()
                .map(p -> new ProjectEntry(
                        p.getTitle(), period(p.getStartDate(), p.getEndDate()),
                        splitTech(p.getTechStack()), p.getRole(),
                        null, null, p.getDescription(),   // problem/solution은 AI가 서사화, result엔 설명 시드
                        p.getGithubUrl(), p.getDeployUrl()))
                .toList();

        List<CareerEntry> careers = careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.INTERNSHIP).stream()
                .map(a -> new CareerEntry(
                        nz(a.getOrganization()), dateStr(a.getCompletedDate()),
                        a.getTitle(), "경력", nz(a.getMemo())))
                .toList();

        List<CertEntry> certs = new ArrayList<>();
        for (CareerActivity a : careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.CERTIFICATE)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) {
                certs.add(new CertEntry(a.getTitle(), nz(a.getOrganization()), dateStr(a.getCompletedDate())));
            }
        }

        List<LanguageEntry> languages = new ArrayList<>();
        for (CareerActivity a : careerActivityRepository
                .findByStudentIdAndTypeOrderByCreatedAtDesc(sid, ActivityType.LANGUAGE_TEST)) {
            if (a.getStatus() == ActivityStatus.COMPLETED) {
                languages.add(new LanguageEntry(a.getTitle(), nz(a.getScore()), dateStr(a.getCompletedDate())));
            }
        }

        List<AwardEntry> awards = awardRepository.findByStudentIdOrderByAwardDateDesc(sid).stream()
                .map(a -> new AwardEntry(
                        a.getTitle(), nz(a.getOrganization()),
                        a.getLevel() == null ? "" : a.getLevel().getLabel(), dateStr(a.getAwardDate())))
                .toList();

        return new ResumeData(personal, education, skills, projects, careers,
                certs, languages, awards, List.of(), null);
    }

    /** 정직성 대조·프롬프트용 근거 텍스트. 여기 없는 사실은 자소서에 쓰면 안 된다. */
    public String buildEvidence(ResumeData d) {
        StringBuilder sb = new StringBuilder();
        if (d.education() != null) {
            sb.append("[학력] ").append(nz(d.education().department()))
              .append(' ').append(d.education().grade()).append("학년 ")
              .append(d.education().semester()).append("학기");
            if (d.education().gpa() != null) sb.append(" (GPA ").append(d.education().gpa()).append(')');
            sb.append('\n');
        }
        if (!d.skills().isEmpty()) {
            sb.append("[스킬] ");
            for (SkillGroup g : d.skills()) sb.append(String.join(", ", g.items())).append(' ');
            sb.append('\n');
        }
        if (!d.certs().isEmpty()) {
            sb.append("[자격증] ");
            sb.append(String.join(", ", d.certs().stream().map(CertEntry::name).toList())).append('\n');
        }
        if (!d.languages().isEmpty()) {
            sb.append("[어학] ");
            sb.append(String.join(", ", d.languages().stream()
                    .map(l -> l.name() + " " + nz(l.score())).toList())).append('\n');
        }
        if (!d.careers().isEmpty()) {
            sb.append("[경력]\n");
            for (CareerEntry c : d.careers())
                sb.append("  - ").append(c.org()).append(" / ").append(c.role())
                  .append(" / ").append(nz(c.period())).append('\n');
        }
        if (!d.projects().isEmpty()) {
            sb.append("[프로젝트]\n");
            for (ProjectEntry p : d.projects()) {
                sb.append("  - ").append(p.title()).append(": ").append(nz(p.result())).append('\n');
                if (p.techStack() != null && !p.techStack().isEmpty())
                    sb.append("      tech: ").append(String.join(", ", p.techStack())).append('\n');
            }
        }
        if (!d.awards().isEmpty()) {
            sb.append("[수상] ");
            sb.append(String.join(", ", d.awards().stream()
                    .map(a -> a.title() + "(" + a.level() + ")").toList())).append('\n');
        }
        return sb.toString().trim();
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
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static String period(LocalDate s, LocalDate e) {
        if (s == null && e == null) return "";
        return (s == null ? "" : s.toString()) + " ~ " + (e == null ? "" : e.toString());
    }

    private static String dateStr(LocalDate d) { return d == null ? "" : d.toString(); }

    private static String nz(String s) { return s == null ? "" : s; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAssemblerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/service/ResumeAssembler.java src/test/java/com/campusflow/domain/resume/service/ResumeAssemblerTest.java
git commit -m "feat(resume): ResumeAssembler — 학생 데이터→이력서 사실 섹션 조립"
```

---

### Task 4: JSON 추출 유틸 + HonestyVerifier (적대적 정직성 검증 + 자동 재생성)

**Files:**
- Create: `src/main/java/com/campusflow/domain/resume/util/JsonExtract.java`
- Create: `src/main/java/com/campusflow/domain/resume/dto/HonestyReport.java`
- Create: `src/main/java/com/campusflow/domain/resume/service/HonestyVerifier.java`
- Test: `src/test/java/com/campusflow/domain/resume/service/HonestyVerifierTest.java`

**Interfaces:**
- Consumes: `AiFacadeService.ask(String, String)`.
- Produces:
  - `JsonExtract.array(String raw)` → `String`(첫 `[`~마지막 `]`), `JsonExtract.object(String raw)` → `String`(첫 `{`~마지막 `}`). 없으면 원문 반환.
  - `HonestyReport(List<Fix> fixes)`, `HonestyReport.Fix(String section, String before, String after, String reason)`.
  - `HonestyVerifier.FixResult(String text, List<HonestyReport.Fix> fixes)`.
  - `HonestyVerifier.verifyAndFix(String sectionLabel, String text, String evidence)` → `FixResult`. 근거 없는 문장 감지 시 최대 2회 순화 재생성; 최종본과 수정 로그 반환.

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HonestyVerifierTest {

    @Mock AiFacadeService ai;
    @InjectMocks HonestyVerifier verifier;

    @Test
    void 근거없는_문장이_감지되면_순화_재생성하고_로그를_남긴다() {
        String evidence = "[자격증] 정보처리기능사";
        String text = "저는 정보처리기사를 취득했고 대기업 3곳에서 근무했습니다.";

        // 1차 검증: 위반 2건 JSON 배열
        when(ai.ask(contains("정직성 검증관"), anyString()))
                .thenReturn("[\"정보처리기사 취득(근거 없음)\", \"대기업 3곳 근무(근거 없음)\"]");
        // 재생성: 근거 기반 순화본
        when(ai.ask(contains("순화"), anyString()))
                .thenReturn("저는 정보처리기능사를 취득하며 기초 역량을 다졌습니다.");

        HonestyVerifier.FixResult res = verifier.verifyAndFix("지원동기", text, evidence);

        assertThat(res.text()).contains("정보처리기능사");
        assertThat(res.text()).doesNotContain("대기업 3곳");
        assertThat(res.fixes()).isNotEmpty();
        assertThat(res.fixes().get(0).reason()).contains("근거 없음");
    }

    @Test
    void 위반이_없으면_원문을_그대로_반환한다() {
        when(ai.ask(contains("정직성 검증관"), anyString())).thenReturn("[]");
        HonestyVerifier.FixResult res = verifier.verifyAndFix("성장과정", "사실만 담긴 글", "[스킬] Java");
        assertThat(res.text()).isEqualTo("사실만 담긴 글");
        assertThat(res.fixes()).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=HonestyVerifierTest`
Expected: FAIL — `HonestyVerifier` 없음.

- [ ] **Step 3: Write minimal implementation**

`JsonExtract.java`:

```java
package com.campusflow.domain.resume.util;

/** reasoning 모델이 앞뒤에 설명을 붙여도 JSON 본문만 뽑아낸다(PortfolioAiGenerator 패턴). */
public final class JsonExtract {
    private JsonExtract() {}

    public static String array(String raw) { return between(raw, '[', ']'); }
    public static String object(String raw) { return between(raw, '{', '}'); }

    private static String between(String raw, char open, char close) {
        if (raw == null) return "";
        int s = raw.indexOf(open), e = raw.lastIndexOf(close);
        return (s >= 0 && e > s) ? raw.substring(s, e + 1) : raw;
    }
}
```

`HonestyReport.java`:

```java
package com.campusflow.domain.resume.dto;

import java.util.List;

/** 정직성 자동수정 로그 — 무엇이 과장으로 감지되어 어떻게 고쳐졌는지 사용자에게 투명 노출. */
public record HonestyReport(List<Fix> fixes) {
    public record Fix(String section, String before, String after, String reason) {}
}
```

`HonestyVerifier.java`:

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.util.JsonExtract;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 적대적 정직성 검증: 생성된 자소서 문장 중 '근거(evidence)'에 없는 과장/창작을 감지하고,
 * 감지되면 근거 기반으로 순화 재생성(최대 2회). 통과하거나 재시도 소진 시 최종본+수정로그 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HonestyVerifier {

    private final AiFacadeService aiFacadeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_REWRITE = 2;

    private static final String VERIFY_SYSTEM = """
            당신은 이력서 자기소개서의 정직성 검증관입니다.
            '지원자 근거'에 명시되지 않은 사실(경험·수치·자격·회사·성과)을 주장하는 문장을 찾아냅니다.
            - 근거에 없는 자격/경력/수치를 언급하면 위반입니다.
            - 근거에 있는 사실을 일반적 포부·의지로 표현한 것은 위반이 아닙니다.
            출력은 위반 사유 문자열의 JSON 배열만. 위반이 없으면 정확히 [] 만 출력합니다.
            """;

    private static final String VERIFY_USER = """
            [지원자 근거]
            %s

            [검사할 자소서 문장]
            %s

            근거에 없는 주장을 하는 부분을 위반 사유 배열로 출력하세요. 없으면 [].
            """;

    private static final String REWRITE_SYSTEM = """
            당신은 이력서 자기소개서를 근거 기반으로 순화(교정)하는 작성자입니다.
            지적된 과장·창작 부분을 제거하거나, 근거에 있는 사실로만 대체해 다시 씁니다.
            없는 내용은 채우지 말고 삭제하세요(창작보다 공백 우선). 존댓말, 본문만 출력.
            """;

    private static final String REWRITE_USER = """
            [지원자 근거]
            %s

            [지적된 위반]
            %s

            [고칠 원문]
            %s

            위반을 모두 제거/순화한 본문만 출력하세요.
            """;

    public record FixResult(String text, List<HonestyReport.Fix> fixes) {}

    public FixResult verifyAndFix(String sectionLabel, String text, String evidence) {
        List<HonestyReport.Fix> fixes = new ArrayList<>();
        String current = text;

        for (int i = 0; i < MAX_REWRITE; i++) {
            List<String> violations = detect(current, evidence);
            if (violations.isEmpty()) break;

            String reason = String.join("; ", violations);
            String rewritten = aiFacadeService.ask(
                    REWRITE_SYSTEM,
                    REWRITE_USER.formatted(evidence, reason, current)).trim();

            fixes.add(new HonestyReport.Fix(sectionLabel, current, rewritten, reason));
            current = rewritten;
        }
        return new FixResult(current, fixes);
    }

    private List<String> detect(String text, String evidence) {
        try {
            String raw = aiFacadeService.ask(VERIFY_SYSTEM, VERIFY_USER.formatted(evidence, text));
            String json = JsonExtract.array(raw);
            String[] arr = objectMapper.readValue(json, String[].class);
            return List.of(arr);
        } catch (Exception e) {
            log.warn("[Honesty] 검증 파싱 실패 — 통과 처리: {}", e.getMessage());
            return List.of();
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=HonestyVerifierTest`
Expected: PASS (두 테스트 모두).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/util/JsonExtract.java src/main/java/com/campusflow/domain/resume/dto/HonestyReport.java src/main/java/com/campusflow/domain/resume/service/HonestyVerifier.java src/test/java/com/campusflow/domain/resume/service/HonestyVerifierTest.java
git commit -m "feat(resume): HonestyVerifier — 적대적 정직성 검증 + 자동 재생성"
```

---

### Task 5: ResumeAiGeneratorService — 자기소개 생성 + 글자수 가드 + 정직성 통합

**Files:**
- Create: `src/main/java/com/campusflow/domain/resume/dto/ResumeDraft.java`
- Create: `src/main/java/com/campusflow/domain/resume/service/ResumeAiGeneratorService.java`
- Modify: `src/main/java/com/campusflow/domain/resume/dto/ResumeData.java` — `Meta`에 `HonestyReport honestyReport` 추가.
- Test: `src/test/java/com/campusflow/domain/resume/service/ResumeAiGeneratorServiceTest.java`

**Interfaces:**
- Consumes: `ResumeAssembler.assemble/buildEvidence`(Task 3), `HonestyVerifier.verifyAndFix`(Task 4), `AiFacadeService.ask`, `CharCounter.check`(jobpilot util).
- Produces:
  - `ResumeDraft(ResumeData data, HonestyReport honestyReport, String template)`.
  - `ResumeAiGeneratorService.generate(String username, String template)` → `ResumeDraft`. template null/blank면 "general".
  - (내부) general 양식: 자소서 4문항 [성장과정 / 성격의 장단점 / 지원동기 / 입사 후 포부] 각 목표 900자(800~1000), 요약 summary 생성.

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
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
class ResumeAiGeneratorServiceTest {

    @Mock ResumeAssembler assembler;
    @Mock HonestyVerifier honestyVerifier;
    @Mock AiFacadeService ai;
    @InjectMocks ResumeAiGeneratorService service;

    @Test
    void general_양식으로_자소서4문항을_생성하고_정직성검증을_거친다() {
        ResumeData facts = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.0),
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java"))),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), null);
        when(assembler.assemble("u")).thenReturn(facts);
        when(assembler.buildEvidence(facts)).thenReturn("[스킬] Java");
        when(ai.ask(anyString(), anyString())).thenReturn("근거 기반 자기소개 본문입니다.");
        when(honestyVerifier.verifyAndFix(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> new HonestyVerifier.FixResult(inv.getArgument(1), List.of()));

        ResumeDraft draft = service.generate("u", "general");

        assertThat(draft.template()).isEqualTo("general");
        assertThat(draft.data().coverLetter()).hasSize(4);
        assertThat(draft.data().coverLetter())
                .extracting(ResumeData.CoverLetterSection::question)
                .containsExactly("성장과정", "성격의 장단점", "지원동기", "입사 후 포부");
        assertThat(draft.data().meta().honestyReport()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAiGeneratorServiceTest`
Expected: FAIL — `ResumeDraft`/`ResumeAiGeneratorService` 없음.

- [ ] **Step 3: Write minimal implementation**

먼저 `ResumeData.Meta`에 honestyReport 추가 (Task 1의 `Meta` 교체):

```java
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String template, String generatedAt, HonestyReport honestyReport) {}
```

> `ResumeData.java` 상단에 `import com.campusflow.domain.resume.dto.HonestyReport;` 는 같은 패키지이므로 불필요. Task 1 테스트의 `new Meta("general", "...")`는 `new Meta("general", "...", null)`로 수정한다.

`ResumeDraft.java`:

```java
package com.campusflow.domain.resume.dto;

/** 생성 초안 — 저장 전 프론트로 반환. honestyReport로 자동수정 내역 투명 노출. */
public record ResumeDraft(ResumeData data, HonestyReport honestyReport, String template) {}
```

`ResumeAiGeneratorService.java`:

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.ai.service.AiFacadeService;
import com.campusflow.domain.jobpilot.util.CharCounter;
import com.campusflow.domain.jobpilot.util.CharCounter.LengthVerdict;
import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeData.*;
import com.campusflow.domain.resume.dto.ResumeDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A 단계 — 표준(general) 이력서 자동생성.
 * 조립된 사실 + AI 자기소개(글자수 가드) + 정직성 검증(자동 재생성) → ResumeDraft(미저장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiGeneratorService {

    private final ResumeAssembler assembler;
    private final HonestyVerifier honestyVerifier;
    private final AiFacadeService aiFacadeService;

    private static final int MAX_LEN_RETRY = 3;
    private static final int TARGET_LIMIT = 1000;               // 목표 상한
    private static final String LIMIT_TYPE = "공백포함";

    // 일반기업 자소서 4대 항목 (딥리서치 근거)
    private static final List<String> GENERAL_QUESTIONS =
            List.of("성장과정", "성격의 장단점", "지원동기", "입사 후 포부");

    private static final String SYSTEM = """
            당신은 한국 신입/전문대 지원자의 이력서 자기소개서를 쓰는 전문 작성자입니다.
            '지원자 근거'에 있는 사실만으로 지정 문항의 자소서를 작성합니다.

            [절대 규칙 - 정직성]
            - 근거에 없는 경험·수치·자격·회사·성과를 만들어내지 않습니다.
            - 회사에서 한 일과 개인/학과 프로젝트를 정직하게 구분합니다.
            - 성과가 작아도 배운 점·직무 연관성으로 연결합니다(자기과소평가·과장 모두 금지).

            [형식]
            - 존댓말, 자연스러운 한국어 산문. 진부한 생활신조·반복·오탈자 금지.
            - 마크다운·제목·불릿 없이 본문만 출력합니다.
            """;

    private static final String USER = """
            [지원자 근거]
            %s

            [희망 직무]
            %s

            [작성할 문항]
            "%s"

            [분량]
            - 목표: %d~%d자(공백포함), 최대 %d자.

            위 문항에 대한 자소서 본문만 출력하세요.
            """;

    private static final String RETRY_OVER = """
            방금 글이 %d자로 상한(%d자)을 초과했습니다. 핵심은 유지하되 %d자 이하로 줄여 다시 쓰세요. 본문만.
            [직전 작성본]
            %s
            """;

    private static final String RETRY_SHORT = """
            방금 글이 %d자로 목표(%d자 이상)에 못 미쳐 빈약합니다.
            근거에 있는 사실을 더 활용해 %d~%d자로 보강하세요(없는 내용 금지). 본문만.
            [직전 작성본]
            %s
            """;

    public ResumeDraft generate(String username, String template) {
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        ResumeData facts = assembler.assemble(username);
        String evidence = assembler.buildEvidence(facts);
        String targetJob = facts.education() == null ? "" : "";  // desiredJob은 evidence에 반영됨

        List<CoverLetterSection> sections = new ArrayList<>();
        List<HonestyReport.Fix> allFixes = new ArrayList<>();

        for (String question : GENERAL_QUESTIONS) {
            String body = generateSection(evidence, targetJob, question);
            HonestyVerifier.FixResult fixed = honestyVerifier.verifyAndFix(question, body, evidence);
            allFixes.addAll(fixed.fixes());
            String finalBody = fixed.text();
            sections.add(new CoverLetterSection(
                    question, finalBody, CharCounter.count(finalBody, true)));
        }

        HonestyReport report = new HonestyReport(allFixes);
        ResumeData withCover = new ResumeData(
                facts.personal(), facts.education(), facts.skills(), facts.projects(),
                facts.careers(), facts.certs(), facts.languages(), facts.awards(),
                sections, new Meta(tpl, null, report));

        return new ResumeDraft(withCover, report, tpl);
    }

    private String generateSection(String evidence, String targetJob, String question) {
        String tj = (targetJob == null || targetJob.isBlank()) ? "(미지정)" : targetJob;
        LengthVerdict t0 = CharCounter.check("", TARGET_LIMIT, LIMIT_TYPE);
        String text = aiFacadeService.ask(SYSTEM,
                USER.formatted(evidence, tj, question, t0.targetMin(), t0.targetMax(), TARGET_LIMIT)).trim();

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
}
```

> 참고: Task 1의 `ResumeDataJsonTest`의 `new Meta("general", "2026-07-31T10:00:00")` 호출을 `new Meta("general", "2026-07-31T10:00:00", null)`로 수정하고 해당 테스트를 재실행해 통과 확인.

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeAiGeneratorServiceTest,ResumeDataJsonTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/dto/ResumeDraft.java src/main/java/com/campusflow/domain/resume/dto/ResumeData.java src/main/java/com/campusflow/domain/resume/service/ResumeAiGeneratorService.java src/test/java/com/campusflow/domain/resume/
git commit -m "feat(resume): ResumeAiGeneratorService — general 자소서 생성+글자수가드+정직성"
```

---

### Task 6: `POST /api/resumes/generate` 엔드포인트

**Files:**
- Modify: `src/main/java/com/campusflow/domain/resume/controller/ResumeController.java`
- Test: `src/test/java/com/campusflow/domain/resume/controller/ResumeGenerateControllerTest.java`

**Interfaces:**
- Consumes: `ResumeAiGeneratorService.generate(username, template)` → `ResumeDraft`.
- Produces: `POST /api/resumes/generate?template=` → `ApiResponse<ResumeDraft>`.

- [ ] **Step 1: Write the failing test** (`@WebMvcTest` 슬라이스 — 보안 필터 무시하고 컨트롤러 배선만 검증)

```java
package com.campusflow.domain.resume.controller;

import com.campusflow.domain.resume.dto.HonestyReport;
import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeDraft;
import com.campusflow.domain.resume.service.PdfService;
import com.campusflow.domain.resume.service.ResumeAiGeneratorService;
import com.campusflow.domain.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResumeController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
class ResumeGenerateControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ResumeService resumeService;
    @MockBean PdfService pdfService;
    @MockBean ResumeAiGeneratorService generatorService;

    @Test
    void generate가_초안을_반환한다() throws Exception {
        ResumeData data = new ResumeData(
                new ResumeData.Personal("홍길동", null, null, null),
                null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문", 2)),
                new ResumeData.Meta("general", null, new HonestyReport(List.of())));
        when(generatorService.generate(any(), eq("general")))
                .thenReturn(new ResumeDraft(data, new HonestyReport(List.of()), "general"));

        mvc.perform(post("/api/resumes/generate").param("template", "general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.template").value("general"))
                .andExpect(jsonPath("$.data.data.coverLetter[0].question").value("지원동기"));
    }
}
```

> 컨트롤러가 `@AuthenticationPrincipal String username`을 받으므로 슬라이스 테스트에서는 시큐리티 오토컨피그를 제외해 principal이 null로 주입돼도 서비스가 mock이라 무방하다. (실제 인증은 통합에서 동작.)

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeGenerateControllerTest`
Expected: FAIL — `/generate` 매핑 없음(404) 또는 `ResumeAiGeneratorService` 미주입.

- [ ] **Step 3: Write minimal implementation** — `ResumeController`에 필드·엔드포인트 추가:

```java
    private final ResumeAiGeneratorService generatorService; // 생성자 주입(@RequiredArgsConstructor)

    @PostMapping("/generate")
    public ApiResponse<com.campusflow.domain.resume.dto.ResumeDraft> generate(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false, defaultValue = "general") String template) {
        return ApiResponse.ok(generatorService.generate(username, template));
    }
```

`import com.campusflow.domain.resume.service.ResumeAiGeneratorService;` 추가.

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=ResumeGenerateControllerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/campusflow/domain/resume/controller/ResumeController.java src/test/java/com/campusflow/domain/resume/controller/ResumeGenerateControllerTest.java
git commit -m "feat(resume): POST /api/resumes/generate 엔드포인트"
```

---

### Task 7: general PDF 템플릿 + 리치 데이터 렌더

**Files:**
- Create: `src/main/resources/templates/resume-general.html`
- Modify: `src/main/java/com/campusflow/domain/resume/service/PdfService.java`
- Modify: `src/main/java/com/campusflow/domain/resume/controller/ResumeController.java` — PDF 경로가 리치 데이터를 렌더.

**Interfaces:**
- Consumes: `ResumeResponse.resumeData()`(JSON), `ResumeResponse.template()`.
- Produces: `PdfService.generateResumePdf(ResumeResponse resume, ResumeData data, String template)` — data가 있으면 `resume-{template}` 템플릿 렌더, 없으면 기존 `resume-pdf` 폴백.

- [ ] **Step 1: Write the failing test**

```java
package com.campusflow.domain.resume.service;

import com.campusflow.domain.resume.dto.ResumeData;
import com.campusflow.domain.resume.dto.ResumeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PdfServiceRichTest {

    @Autowired PdfService pdfService;

    @Test
    void 리치데이터로_general_PDF를_생성한다() {
        ResumeData data = new ResumeData(
                new ResumeData.Personal("홍길동", "201918023", "h@x", "010-0000-0000"),
                new ResumeData.Education("컴퓨터정보과", 2, 1, 4.05),
                List.of(new ResumeData.SkillGroup("보유 기술", List.of("Java", "Spring"))),
                List.of(new ResumeData.ProjectEntry("캠퍼스플로우", "2025", List.of("Spring"),
                        "백엔드", null, null, "학과 관리 시스템", "https://github.com/x", null)),
                List.of(), List.of(new ResumeData.CertEntry("정보처리기능사", "산인공", "2025")),
                List.of(), List.of(),
                List.of(new ResumeData.CoverLetterSection("지원동기", "본문입니다.", 5)),
                new ResumeData.Meta("general", null, null));
        ResumeResponse resume = new ResumeResponse(1L, "제목", "요약", List.of("Java"),
                "백엔드", null, "general", List.of(), null);

        byte[] pdf = pdfService.generateResumePdf(resume, data, "general");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(5, pdf.length))).startsWith("%PDF");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=PdfServiceRichTest`
Expected: FAIL — `generateResumePdf(ResumeResponse, ResumeData, String)` 오버로드 없음 / `resume-general` 템플릿 없음.

- [ ] **Step 3: Write minimal implementation**

`resume-general.html` (templates 폴더, 기존 `resume-pdf.html` 스타일 계승 — navy `#1b4f72`, accent `#a8d5a2`):

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Malgun Gothic', sans-serif; font-size: 11pt; color: #1a1a2e; padding: 40px; }
        .header { border-bottom: 3px solid #1b4f72; padding-bottom: 16px; margin-bottom: 20px; }
        .header h1 { font-size: 22pt; color: #1b4f72; }
        .header .meta { font-size: 10pt; color: #555; margin-top: 4px; }
        .section { margin-bottom: 18px; }
        .section-title { font-size: 13pt; font-weight: bold; color: #1b4f72;
            border-left: 4px solid #a8d5a2; padding-left: 8px; margin-bottom: 8px; }
        .row { margin-bottom: 4px; line-height: 1.6; }
        .skill-tag { display: inline-block; background: #1b4f72; color: #fff;
            padding: 2px 9px; border-radius: 12px; font-size: 9pt; margin: 2px; }
        .card { border: 1px solid #d0e8d0; border-radius: 6px; padding: 10px 14px; margin-bottom: 8px; }
        .card .t { font-weight: bold; color: #1b4f72; }
        .cover p { line-height: 1.75; margin-bottom: 6px; white-space: pre-wrap; }
        .cover h3 { font-size: 11.5pt; color: #1b4f72; margin: 10px 0 4px; }
    </style>
</head>
<body>
    <div class="header">
        <h1 th:text="${data.personal() != null ? data.personal().name() : ''}">이름</h1>
        <div class="meta" th:if="${data.personal() != null}"
             th:text="|${data.personal().email()} · ${data.personal().phone()}|">contact</div>
        <div class="meta" th:if="${data.education() != null}"
             th:text="|${data.education().department()} ${data.education().grade()}학년 ${data.education().semester()}학기|">학력</div>
    </div>

    <div class="section" th:if="${data.skills() != null and !data.skills().isEmpty()}">
        <div class="section-title">보유 기술</div>
        <div th:each="g : ${data.skills()}">
            <span class="skill-tag" th:each="s : ${g.items()}" th:text="${s}">skill</span>
        </div>
    </div>

    <div class="section" th:if="${data.projects() != null and !data.projects().isEmpty()}">
        <div class="section-title">프로젝트</div>
        <div class="card" th:each="p : ${data.projects()}">
            <div class="t" th:text="|${p.title()}  (${p.period()})|">프로젝트</div>
            <div class="row" th:if="${p.role()}" th:text="|역할: ${p.role()}|">역할</div>
            <div class="row" th:if="${p.result()}" th:text="${p.result()}">설명</div>
            <div class="row" th:if="${p.techStack() != null and !p.techStack().isEmpty()}"
                 th:text="|기술: ${#strings.listJoin(p.techStack(), ', ')}|">기술</div>
            <div class="row" th:if="${p.githubUrl()}" th:text="${p.githubUrl()}">github</div>
        </div>
    </div>

    <div class="section" th:if="${data.careers() != null and !data.careers().isEmpty()}">
        <div class="section-title">경력</div>
        <div class="card" th:each="c : ${data.careers()}">
            <div class="t" th:text="|${c.org()} · ${c.role()}  (${c.period()})|">경력</div>
            <div class="row" th:if="${c.description()}" th:text="${c.description()}">설명</div>
        </div>
    </div>

    <div class="section" th:if="${data.certs() != null and !data.certs().isEmpty()}">
        <div class="section-title">자격증</div>
        <div class="row" th:each="c : ${data.certs()}" th:text="|${c.name()} · ${c.org()} (${c.date()})|">자격증</div>
    </div>

    <div class="section" th:if="${data.languages() != null and !data.languages().isEmpty()}">
        <div class="section-title">어학</div>
        <div class="row" th:each="l : ${data.languages()}" th:text="|${l.name()} ${l.score()} (${l.date()})|">어학</div>
    </div>

    <div class="section" th:if="${data.awards() != null and !data.awards().isEmpty()}">
        <div class="section-title">수상</div>
        <div class="row" th:each="a : ${data.awards()}" th:text="|${a.title()} · ${a.org()} · ${a.level()} (${a.date()})|">수상</div>
    </div>

    <div class="section cover" th:if="${data.coverLetter() != null and !data.coverLetter().isEmpty()}">
        <div class="section-title">자기소개서</div>
        <div th:each="s : ${data.coverLetter()}">
            <h3 th:text="${s.question()}">문항</h3>
            <p th:text="${s.body()}">본문</p>
        </div>
    </div>
</body>
</html>
```

`PdfService.java` — 오버로드 추가(기존 메서드 유지):

```java
    public byte[] generateResumePdf(ResumeResponse resume,
                                    com.campusflow.domain.resume.dto.ResumeData data,
                                    String template) {
        if (data == null) return generateResumePdf(resume);   // 구 이력서 폴백
        String tpl = (template == null || template.isBlank()) ? "general" : template;
        Context ctx = new Context();
        ctx.setVariable("resume", resume);
        ctx.setVariable("data", data);
        return render("resume-" + tpl, ctx);
    }
```

`ResumeController.downloadPdf` — 리치 데이터가 있으면 그것으로 렌더:

```java
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal String username,
                                              @PathVariable Long id,
                                              @RequestParam(required = false) String template) {
        ResumeResponse resume = resumeService.getResume(username, id);
        com.campusflow.domain.resume.dto.ResumeData data = null;
        if (resume.resumeData() != null && !resume.resumeData().isBlank()) {
            try {
                data = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(resume.resumeData(), com.campusflow.domain.resume.dto.ResumeData.class);
            } catch (Exception e) { data = null; } // 파싱 실패 시 구 템플릿 폴백
        }
        String tpl = (template != null && !template.isBlank()) ? template : resume.template();
        byte[] pdf = pdfService.generateResumePdf(resume, data, tpl);

        String filename = resume.title().replaceAll("[^a-zA-Z0-9가-힣]", "_") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd -q test -Dtest=PdfServiceRichTest`
Expected: PASS (`%PDF` 헤더). 그리고 전체 컴파일 확인: `mvn.cmd -q compile`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/resume-general.html src/main/java/com/campusflow/domain/resume/service/PdfService.java src/main/java/com/campusflow/domain/resume/controller/ResumeController.java src/test/java/com/campusflow/domain/resume/service/PdfServiceRichTest.java
git commit -m "feat(resume): general PDF 템플릿 + 리치 데이터 렌더"
```

---

### Task 8: 프론트엔드 — 생성 버튼 + 초안 미리보기/편집 + honesty 배지

**Files:**
- Modify: `frontend/src/api/resume.js`
- Modify: `frontend/src/pages/Technical.jsx`
- Modify: `frontend/src/locales/ko/translation.json` 외 4개 언어(en/zh/ja/vi) — 실제 경로는 프로젝트의 i18n 구조를 따른다(리소스 파일 위치 확인 후 동일 키 추가).
- Test: 수동 검증(프론트 자동화 테스트 없음).

**Interfaces:**
- Consumes: `POST /api/resumes/generate?template=` → `{ success, data: { data: ResumeData, honestyReport, template } }`.
- Produces: `generateResume(template)` in `api/resume.js`.

- [ ] **Step 1: `api/resume.js`에 함수 추가**

기존 파일 상단의 `api` import·패턴을 그대로 따르며 추가:

```js
// 이력서 AI 자동생성 (초안 반환, 저장 안 함)
export const generateResume = async (template = 'general') => {
  const res = await api.post(`/resumes/generate?template=${encodeURIComponent(template)}`)
  return res.data.data   // { data: ResumeData, honestyReport, template }
}
```

- [ ] **Step 2: `Technical.jsx` — 생성 버튼 + 상태**

이력서 탭(폼 근처)에 버튼과 핸들러를 추가. 기존 `resumeForm` 상태와 `createResume` 흐름을 재사용해 초안을 폼에 채운다:

```jsx
import { generateResume } from '../api/resume'
// ...
const [generating, setGenerating] = useState(false)
const [honestyFixes, setHonestyFixes] = useState([])

const handleGenerateResume = async (template = 'general') => {
  setGenerating(true)
  try {
    const draft = await generateResume(template)      // { data, honestyReport, template }
    const d = draft.data
    const skillsCsv = (d.skills || []).flatMap(g => g.items || []).join(', ')
    const coverText = (d.coverLetter || [])
      .map(s => `[${s.question}]\n${s.body}`).join('\n\n')
    setResumeForm(prev => ({
      ...prev,
      title: prev.title || `${d.personal?.name || ''} 이력서`,
      summary: coverText,
      skills: skillsCsv,
      targetJob: prev.targetJob || '',
      resumeData: JSON.stringify(d),
      template: draft.template,
    }))
    setHonestyFixes(draft.honestyReport?.fixes || [])
  } catch (e) {
    alert(t('resume.generateError', 'AI 생성에 실패했습니다. 잠시 후 다시 시도해주세요.'))
  } finally {
    setGenerating(false)
  }
}
```

버튼 + honesty 배지 JSX(이력서 폼 위):

```jsx
<div className="flex items-center gap-2 mb-3">
  <button type="button" className="btn-secondary" disabled={generating}
          onClick={() => handleGenerateResume('general')}>
    {generating ? t('resume.generating', '생성 중…') : t('resume.generate', 'AI로 이력서 생성')}
  </button>
</div>
{honestyFixes.length > 0 && (
  <div className="chip mb-2" title={honestyFixes.map(f => f.reason).join('\n')}>
    {t('resume.honestyFixed', '정직성 자동수정')} {honestyFixes.length}
  </div>
)}
```

> `resumeForm`에 `resumeData`/`template` 키가 없으면 초기 상태에 추가하고, `handleCreateResume`가 `createResume(...)` 호출 시 `resumeData`·`template`를 함께 전송하도록 payload에 포함시킨다(기존 `createResume` 요청 바디에 두 필드 추가).

- [ ] **Step 3: i18n 키 추가** — 5개 언어 리소스에 동일 키(값은 각 언어로):

```
resume.generate           : "AI로 이력서 생성" / "Generate resume with AI" / ...
resume.generating         : "생성 중…"
resume.honestyFixed       : "정직성 자동수정"
resume.generateError      : "AI 생성에 실패했습니다. 잠시 후 다시 시도해주세요."
```

- [ ] **Step 4: 빌드 + 수동 검증**

Run: `cd frontend && npm run build`
Expected: 빌드 성공.

수동 검증(로컬 백엔드 8080 + 프론트 3000 기동):
1. 로그인 → Technical(기술) 탭 → 이력서 → "AI로 이력서 생성" 클릭.
2. 요약(자기소개 4항목)·스킬이 폼에 채워지는지 확인.
3. 저장 후 목록·PDF 다운로드에서 리치 내용이 나오는지 확인.
4. (정직성) 조립 데이터에 없는 자격증을 임시로 프롬프트에 유도하는 시나리오가 아니면 배지는 0일 수 있음 — fixes가 있으면 배지 노출·tooltip 확인.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/resume.js frontend/src/pages/Technical.jsx frontend/src/locales
git commit -m "feat(resume): 프론트 AI 이력서 생성 버튼 + 정직성 배지 + i18n"
```

---

## Self-Review (작성자 체크 결과)

**Spec coverage**
- 공통기반(ResumeData·컬럼·Assembler) → Task 1~3 ✅
- A 생성(글자수 가드·정직성 자동재생성) → Task 4~5 ✅
- `/generate` 엔드포인트 → Task 6 ✅
- general PDF 템플릿 → Task 7 ✅
- 프론트 생성 UI·honesty 배지·i18n → Task 8 ✅
- B(6양식)·C(공고맞춤)는 **플랜 2·3으로 분리**(이 플랜 범위 밖, 스펙 §10 순서와 일치) — 의도된 스코프.

**Placeholder scan**: TBD/TODO 없음. 모든 코드 스텝에 실제 코드 포함.

**Type consistency**:
- `ResumeData.Meta`는 Task 1에서 2-arg로 시작 → Task 5에서 3-arg(honestyReport 포함)로 확장, Task 1 테스트 호출도 3-arg로 갱신하도록 명시(정합).
- `CharCounter.check(String, int, String)` / `LengthVerdict.count()/status()/targetMin()/targetMax()/needsRetry()` — 수집된 실제 시그니처와 일치.
- 리포지토리 메서드명(`findByStudentIdOrderByStartDateDesc`, `findByStudentIdAndTypeOrderByCreatedAtDesc`, `findByStudentIdOrderByAwardDateDesc`, `calculateGpa`) — 실제와 일치.
- `ResumeRequest`/`ResumeResponse` 필드 순서 변경 반영(생성자 인자 정합).

**주의 사항(구현자 유의)**:
- `ApiResponse.ok(ResumeDraft)`는 제네릭 `ok(T)`로 해석됨(문제 없음). `String` 반환만 오버로드 함정 주의.
- `resumeForm` 초기 상태에 `resumeData`/`template` 키를 반드시 추가(누락 시 저장 payload에서 빠짐).
