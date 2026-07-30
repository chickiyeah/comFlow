# 이력서 자동생성 (리치 이력서) — 설계 문서

작성일: 2026-07-30
대상 시스템: CampusFlow (`domain/resume`, `domain/jobpilot`, `domain/career`)

## 1. 배경 / 목표

현재 `Resume` 엔티티는 `title / summary / skills(CSV) / targetJob + 포트폴리오 링크`뿐이라 실제 취업용 이력서로서 깊이가 없다. 학생이 이미 시스템에 축적한 데이터(성적·자격증·어학·인턴·수상·포트폴리오)를 근거로 **실무에서 통하는 완성형 이력서를 자동 생성**한다.

세 가지를 모두 만든다:

- **A. 완성형 표준 이력서** — 전 섹션이 채워진 국문 이력서 1벌을 원클릭 자동 생성 → 편집 → 저장.
- **B. 여러 양식 스타일** — 같은 데이터를 양식(`dev` 개발자형 / `ncs` 공기업 NCS형 / `general` 일반기업형)만 바꿔 렌더. 양식별 강조 섹션·자기소개 톤·PDF 레이아웃이 다름.
- **C. 공고 맞춤 여러 벌** — 저장공고(`SavedJob`)를 골라 `JobMatcherService` 매칭 → 그 직무에 맞게 강조점 조정한 이력서 생성.

**설계 원칙**: 자동 저장 없음(항상 사람 검토·편집), 사실은 조립 데이터에서만(정직성 가드), 생성 후 적대적 정직성 검증 패스.

## 2. 리서치 근거 (딥리서치 3표 적대적 검증 통과)

아래 기준을 양식별 루브릭·프롬프트·검증 규칙의 근거로 삼는다.

### 2.1 개발자/IT 이력서
- 섹션 순서: **인적사항 → 기술스택 → 프로젝트 → 경험/활동 → 학력·자격증**, 전체 1~2장. (비전공/신입은 학력 하단)
- 프로젝트: `프로젝트명·기간·사용기술·역할/기여·문제상황·해결방법·정량화된 결과`. **문제→역할→해결→전후 변화** 서사. 나열보다 **대표 2~4개 심화**.
- 강조: 실제 기술스택 경험, 문제해결 과정, GitHub/포트폴리오 공개·협업. GPA·학력은 기본 필터용(중요도 낮음).
- 출처: JobKorea 개발자 이력서 가이드, Linkareer, 우아한형제들 기술블로그 (교차 확인, high).

### 2.2 공기업 NCS 블라인드 입사지원서
- 5개 직무중심 섹션: **(1) 인적사항 (2) 우대·결격사항 (3) 직무 관련 경력·경험 (4) 직무 관련 교육 (5) 직무 관련 자격증** + 경험·경력 기술서 + 자기소개서.
- 인적사항: **성명·출생월일·이메일·연락처만**. 사진·나이·전체 생년월일 없음.
- **블라인드 하드룰**: 성별·학력·나이·출신지역·가족관계·신체조건·종교·**출신학교명**을 직·간접 기재 시 불합격 불이익. → 생성기가 이 항목을 **절대 출력하지 않도록** 강제.
- 경력(유급 근로) vs 경험(동아리·봉사·프로젝트 등 무급 활동) 구분 → 신입/경험부족자 보완 경로.
- 분량: 경험·경력 기술서 500~1,200자, 자기소개 각 문항 200~600자, STAR 구조.
- 출처: alio.go.kr 공식 입사지원서 원문, ncs.go.kr, 워크넷 블라인드 가이드 (high).

### 2.3 일반기업 국문 이력서·자기소개서
- 자소서 4대 항목: **성장과정 · 성격 장단점 · 지원동기 · 입사후포부**. 포부는 정량화 권장.
- 분량: 이력서 최대 2장, 자소서 A4 1~2장, 항목 통상 800~1,000자, **요구 글자수 90%+ 충족**이 성실 신호.
- 출처: jge.go.kr 공식 자소서 작성요령, JobKorea, Saramin (high).

### 2.4 공통 안티패턴 / STAR
- 감점: **오탈자(치명적)**, 진부한 생활신조, 반복 표현, **과장·거짓**, 자기과소평가, 필수항목 누락(76%), 산만/요점불분명(51.5%).
- STAR: **Action이 가장 중요**(구체 행동·도구·협업). 결과는 정량화하되, 성과가 작아도 **배운 점·직무 연관성**을 강조해 신입 보완.
- AI 생성 시: 조립된 사실만 사용, 과장·창작 금지.

## 3. 아키텍처

```
학생 데이터                     생성 파이프라인                         출력
─────────                  ────────────────────                    ────
Student/Grade  ─┐
Portfolio      ─┤   ResumeAssembler.assemble(username)
CareerActivity ─┼─▶ (ProfileAssembler 확장)  ──▶ ResumeData(조립,사실)
Award          ─┤          │
Resume(기존)   ─┘          ▼
                    ResumeAiGeneratorService.generate(data, template, jobMatch?)
                           │  ├ 양식별 systemPrompt (dev/ncs/general)
                           │  ├ AiFacadeService.ask()  (자기소개·요약)
                           │  ├ 글자수 가드 루프 (JobPilot CoverLetterGenerator 패턴)
                           │  └ 정직성 적대적 검증 패스 (HonestyVerifier)
                           ▼
                    ResumeDraft(JSON) ──▶ Resume.resume_data 저장 ──▶ 양식별 PDF
```

핵심 재사용: `ProfileAssembler`(jobpilot), `JobMatcherService`(jobpilot), `AiFacadeService`(캐싱·게이트웨이), `PdfService`(HTML→PDF), JobPilot의 글자수 가드·정직성 가드 패턴.

## 4. 데이터 모델 (additive)

`Resume` 엔티티에 컬럼 하나 추가 (ddl-auto=update가 자동 생성):

```java
@Column(columnDefinition = "TEXT")
private String resumeData;   // ResumeData JSON 직렬화
@Column(length = 20)
private String template;     // dev | ncs | general (기본 general)
private Long sourceJobId;    // C: 맞춤 생성의 원본 SavedJob (nullable)
```

기존 `title/summary/skills/targetJob/포트폴리오`는 **호환 유지**(리스트·PDF 하위호환). 신규 리치 데이터는 `resumeData` JSON에 전 섹션 저장.

`ResumeData` (DTO, JSON):
```
personal   { name, studentId?, email, phone }   // ncs면 studentId/사진 제외
education  { department, grade, semester, gpa?, admission? }  // ncs면 gpa/학교명 정책 반영
skills     [ { category, items[] } ]
projects   [ { title, period, techStack[], role, problem, solution, result } ]  // 2~4개
careers    [ { org, period, role, type: 경력|경험, description } ]
certs      [ { name, org, date } ]
languages  [ { name, score, date } ]
awards     [ { title, org, level, date } ]
coverLetter[ { question, body, charCount } ]   // 양식별 항목 세트
meta       { template, generatedAt, honestyReport }
```

## 5. 컴포넌트

### 5.1 ResumeAssembler (신규, `domain/resume/service`)
`ProfileAssembler`를 확장/재사용해 학생 데이터를 `ResumeData`의 **사실 섹션**(personal/education/skills/projects/careers/certs/languages/awards)으로 조립. 이 단계는 **AI 없음** — DB 사실만. coverLetter는 비워둠.

### 5.2 ResumeAiGeneratorService (신규)
1. `ResumeAssembler`로 사실 조립 → `asEvidenceBlock()`(근거 텍스트).
2. 양식별 systemPrompt 선택:
   - `dev`: 프로젝트 서사 강조, 기술스택 상단, 학력 하단, GPA 비중 축소.
   - `ncs`: 블라인드 하드룰(사진·나이·학교명·출신지역·가족 등 금지), 경력/경험 구분, STAR 자소서 4문항(각 200~600자), 직무능력 중심.
   - `general`: 자소서 4항목(성장과정·성격 장단점·지원동기·입사후포부, 각 800~1,000자, 요구치 90%+), 포부 정량화.
3. `AiFacadeService.ask()`로 자기소개·요약 생성 → JSON 파싱(reasoning 모델 방어, `PortfolioAiGeneratorService` 패턴).
4. **글자수 가드 루프**(JobPilot `CoverLetterGeneratorService` 계승): 목표 분량 85~98% 벗어나면 최대 3회 재생성.
5. **정직성 적대적 검증**(5.3) 통과 → `ResumeDraft` 반환(저장 안 함, 프론트로).

### 5.3 HonestyVerifier (신규 — 적대적 검증)
생성 결과의 각 사실성 문장을 `evidenceBlock`과 대조. 별도 AI 검증 프롬프트가 "이 문장을 뒷받침하는 근거가 조립 데이터에 있는가? 없으면 과장/창작으로 플래그"를 판정. 플래그된 문장은 (a) 근거 기반으로 자동 순화 재생성 또는 (b) `honestyReport`에 경고로 첨부해 사용자에게 노출. 안티패턴(오탈자·진부표현·반복)도 규칙+AI로 점검. → 사용자가 "과장 없음" 신뢰하고 편집 가능.

### 5.4 양식별 PDF 템플릿 (`PdfService`)
`resume-dev.html` / `resume-ncs.html` / `resume-general.html` 3종. 시스템 Malgun Gothic. NCS 템플릿은 사진·나이·학교명 필드 자체가 없음. `GET /api/resumes/{id}/pdf?template=` 로 선택 렌더(기본은 저장된 template).

## 6. API

```
POST /api/resumes/generate?template=dev|ncs|general
     body: { targetJob? }              → ResumeDraft (미저장, honestyReport 포함)
POST /api/resumes/generate-for-job/{savedJobId}?template=
     → JobMatcher 매칭 → 강조 조정된 ResumeDraft (+ matchReport)
POST /api/resumes            (기존) 편집본 저장 — resumeData/template/sourceJobId 수용
GET  /api/resumes/{id}/pdf?template=   양식별 PDF
```
`@AuthenticationPrincipal String username` (CLAUDE.md 규칙).

## 7. 프론트엔드

- `Technical.jsx` 이력서 탭: **"AI로 이력서 생성"** 버튼 + 양식 선택(개발자/공기업NCS/일반기업). 생성 → 섹션별 미리보기·인라인 편집 → 저장. `honestyReport` 경고 배지 표시.
- `Career.jsx`(또는 저장공고 목록): 저장공고 카드에 **"이 공고로 이력서 생성"** → C 파이프라인 → matchReport 배지 + 생성.
- `api/resume.js`: `generateResume(template, body)`, `generateResumeForJob(jobId, template)` 추가.
- i18n 5개언어 키 추가(생성 버튼·양식명·honesty 경고·섹션 라벨).

## 8. 테스트

- ResumeAssembler 단위: 포트폴리오 techStack 합집합, COMPLETED 자격증만, 인턴→경력, 수상 매핑.
- 양식 규칙: `ncs` 출력에 학교명·나이·사진·출신지역·가족 **미포함** 단정(블라인드 하드룰 회귀 테스트). `general` 자소서 4항목 존재·글자수 범위. `dev` 프로젝트 2~4개·정량결과 필드.
- HonestyVerifier: 근거 없는 문장(예: 조립에 없는 자격증) 주입 시 플래그되는지.
- 글자수 가드: 목표 범위 수렴.
- 컴파일(`mvn compile`) + 프론트 `npm build`.

## 9. 범위 밖 (YAGNI)

- 영문/다국어 이력서 생성(국문 우선, 후순위).
- ATS 점수 시뮬레이터, 이력서 A/B 자동 비교.
- 공고 상위 N건 JD 자동추출 랭킹(C는 저장공고 1건 매칭부터; JD추출은 후속).
- 자동 저장/자동 제출(항상 사람 검토).

## 10. 빌드 순서

1. 공통 기반: `ResumeData` 모델 + `Resume` 컬럼 추가 + `ResumeAssembler`.
2. A: `ResumeAiGeneratorService`(general 기본) + `HonestyVerifier` + `/generate` + 프론트 생성 버튼 + `resume-general.html`.
3. B: `dev`/`ncs` 프롬프트·템플릿 + 양식 선택 UI.
4. C: `JobMatcher` 연동 + `/generate-for-job` + 저장공고 버튼.

각 단계 끝에 컴파일·테스트·(로컬 검증). 배포는 사용자 확인 후.
