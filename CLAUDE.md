# CampusFlow — Claude Code 인계 문서

2년제 컴퓨터정보과 학과 통합 관리 시스템. 학생의 성적·출결·포트폴리오·자격증·취업 준비를 AI와 연계해 통합 관리한다.

---

## 실행 명령어

```bash
# 백엔드 (Spring Boot, 포트 8080)
cd C:\Users\0_8_2\comFlow
C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd spring-boot:run

# 프론트엔드 (Vite/React, 포트 3000)
cd C:\Users\0_8_2\comFlow\frontend
npm run dev

# 프론트엔드 빌드
npm run build

# 백엔드 컴파일만
C:\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd compile -q
```

---

## 인프라

| 항목 | 값 |
|------|-----|
| DB | MySQL 8 @ `10.8.0.1:3307` root/jvision, DB명 `campusflow` |
| ChromaDB | `10.8.0.1:8000` |
| 메일 서버 | `10.8.0.17:25` (opendkim DKIM 서명, SPF+DMARC 완료) |
| 발신 주소 | `noreply@campusflow.jvision.org` |
| 서버 SSH | `ssh ruddls030@10.8.0.17` (pw: dlstn0722, sudo 가능) |
| 프론트 도메인 | `campusflow.jvision.org` (Vite allowedHosts 등록됨) |
| **개발 서버** | `10.8.0.2` — rndp/cjm@0124, Docker Compose 운영 |

### 개발 서버 (10.8.0.2) Docker Compose

프로젝트 경로: `/home/ruddls030/campusflow`

```bash
# SSH 접속
ssh rndp@10.8.0.2  # pw: cjm@0124

# 컨테이너 관리
cd /home/ruddls030/campusflow
docker-compose ps               # 상태 확인
docker-compose logs -f          # 전체 로그 스트림
docker-compose logs -f backend  # 백엔드 로그만
docker-compose up -d --build    # 재빌드 후 시작
docker-compose down             # 중지

# 접속 주소
# 백엔드: http://10.8.0.2:8080
# 프론트: http://10.8.0.2:3000
```

**배포 절차 (로컬 수정 후):**
```bash
# 1. 소스 전송 (Git Bash / pscp)
"/c/Program Files/PuTTY/pscp" -pw "cjm@0124" <파일> rndp@10.8.0.2:/home/ruddls030/campusflow/<경로>

# 2. 서버에서 재빌드
ssh rndp@10.8.0.2 "cd /home/ruddls030/campusflow && docker-compose up -d --build"
```

네트워크: `network_mode: host` — 컨테이너가 호스트 네트워크 직접 사용. `localhost:8080`이 백엔드, DB는 10.8.0.1:3307 직접 접근.

---

## 기술 스택

**백엔드**
- Spring Boot 3.3.5 / Java 17 (pom.xml에 `<java.version>17</java.version>`)
- JPA (Hibernate ddl-auto=update, 테이블 자동 생성)
- Spring Security + JWT (jjwt 0.12.6)
- dotenv-java (.env 로드, `CampusFlowApplication.main()`에서 처리)
- RestTemplate 빈 — `config/AppConfig.java`에 `@Bean` 정의됨 (외부 API 호출용)
- RestClient (Spring 6) — `OpenAiService`, `ChromaDbService`에서 사용
- Jsoup 1.17.2 (고용24 HTML 스크래핑)
- Jackson ObjectMapper (JSON 파싱)

**AI**
- OpenAI gpt-4o (primary)
- Claude claude-opus-4-7 (backup fallback)
- `AiFacadeService` — OpenAI 먼저 시도, 실패 시 Claude 호출
- ChromaDB RAG (벡터 검색, 학과 자료 기반)

**프론트엔드**
- React 18 + Vite + Tailwind CSS
- react-i18next (ko/en/zh/ja/vi 5개 언어)
- react-router-dom
- axios (`frontend/src/api/axios.js` — baseURL `/api`, JWT Bearer 자동 주입)

---

## 중요 보안 주의사항

### `@AuthenticationPrincipal`은 반드시 `String`으로 받는다
JWT 필터(`JwtAuthenticationFilter`)가 principal을 `String`(username)으로 저장한다.
`UserDetails`로 받으면 NPE 발생.

```java
// ✅ 올바름
public ApiResponse<?> myMethod(@AuthenticationPrincipal String username) { ... }

// ❌ NPE 발생
public ApiResponse<?> myMethod(@AuthenticationPrincipal UserDetails user) { ... }
```

---

## 도메인 구조 (`src/main/java/com/campusflow/domain/`)

| 도메인 | 설명 |
|--------|------|
| `user` | 인증(JWT), 회원가입, 이메일 인증, GitHub 토큰 저장 |
| `student` | 학생 엔티티 (grade, semester, department) |
| `grade` | 성적 CRUD |
| `attendance` | 출결 CRUD |
| `graduation` | 졸업요건 확인 |
| `portfolio` | 포트폴리오 CRUD + GitHub/파일 AI 생성 |
| `resume` | 이력서 CRUD + PDF 다운로드 |
| `coverletter` | 자기소개서 CRUD |
| `assistant` | AI 학생 어드바이저 분석 + 자기소개서 AI 생성 |
| `award` | 수상내역 |
| `library` | 도서 검색 |
| `suggestion` | 익명 건의함 |
| `roadmap` | AI 취업 로드맵 생성 |
| `career` | 취업 준비 활동 트래킹 + 채용공고 검색 + Q-Net 자격증 |

---

## 취업 관련 서비스 (`domain/career/service/`)

| 서비스 | 설명 | 상태 |
|--------|------|------|
| `CareerActivityService` | 취업 준비 활동 CRUD (자격증/어학/인턴 등) | ✅ |
| `SavedJobService` | 채용공고 스크랩 저장 | ✅ |
| `JobkoreaService` | 잡코리아 내부 API POST (`/Search/api/display/v2/jobs`) | ✅ 작동 |
| `Work24ScraperService` | 고용24 HTML 스크래핑 (Jsoup, 쿠키 세션 방식) | ✅ 작동 |
| `WorknetService` | 워크넷 채용공고 (work24.go.kr UUID 키, 학교계정 필요) | ⚠️ 키 미작동 |
| `QNetService` | Q-Net 자격증 시험일정/종목목록/상세/시험장소 | ✅ 작동 |
| `BlindRecruitService` | NCS 블라인드 채용 기업 (403 — 별도 권한 필요) | ❌ |

### JobSearchController 엔드포인트
```
GET /api/career/search/jobs
  params: keyword, page, source(all|jobkorea|work24|worknet), region, career, empType
  region: 서울|경기|인천|부산|대구|대전|광주|울산|세종|강원|충북|충남|전북|전남|경북|경남|제주
  career: 신입|경력
  empType: 정규직|계약직

GET /api/career/search/certs/schedules   # 시험 일정
GET /api/career/search/certs/list        # 자격증 종목 검색
GET /api/career/search/certs/detail      # 자격증 상세 (jmCd 필요)
GET /api/career/search/certs/locations   # 시험장소
GET /api/career/search/blind-recruit     # 블라인드 채용 기업
```

---

## .env 키 목록

```
DB_URL / DB_USER / DB_PASS
JWT_SECRET
CLAUDE_API_KEY       # sk-ant-...
CLAUDE_MODEL         # claude-opus-4-7
OPENAI_API_KEY       # sk-proj-...
OPENAI_MODEL         # gpt-4o
CHROMA_HOST / CHROMA_PORT
MAIL_HOST / MAIL_PORT / MAIL_FROM
WORKNET_API_KEY      # work24.go.kr UUID (학교계정 재신청 필요)
WORKNET_JOB_DUTY_KEY # 직무정보
WORKNET_JOB_INFO_KEY # 직업정보
QNET_API_KEY         # data.go.kr Decoding 키 (디코딩 상태로 저장)
BLIND_RECRUIT_API_KEY # data.go.kr (현재 403, 별도 권한 필요)
```

---

## GitHub 프라이빗 저장소 연동

사용자가 GitHub Personal Access Token(PAT)을 설정하면 프라이빗 저장소도 AI 포트폴리오 분석 가능.

- 저장: `POST /api/user/github-token` `{ "token": "ghp_..." }`
- 조회 여부: `GET /api/user/github-token`
- 삭제: `DELETE /api/user/github-token`
- `PortfolioController.generateFromGithub()` — 토큰이 있으면 `Authorization: Bearer {token}` 헤더로 GitHub API 호출

필요 권한: `Contents (read)` (Fine-grained token)

---

## 메일 서버 (10.8.0.17) DKIM 설정 현황

| 도메인 | DKIM | SPF | DMARC |
|--------|------|-----|-------|
| decodns.org | ✅ | ✅ | - |
| jvision.org | ✅ | ✅ | - |
| matching.decodns.org | ✅ | ✅ | - |

KeyTable: `/etc/opendkim/KeyTable`
SigningTable: `/etc/opendkim/SigningTable`
키 위치: `/etc/opendkim/keys/{domain}/mail.private`

---

## 디자인 시스템

`frontend/src/tailwind.config.js` — Cyber-Academic Professional 테마.

- Primary: `#00236f` (Deep Navy)
- Secondary-fixed: `#bff365` (Accent Lime) — CTA 버튼, 활성 nav
- 다크모드: `class` 방식 (`useThemeStore` → `document.documentElement.classList`)
- 공통 클래스: `.card`, `.btn-primary`, `.btn-hero`, `.btn-secondary`, `.input`, `.chip`, `.chip-active`
- Shadow: `shadow-card` (navy tint), `shadow-lime` (lime glow)

---

## 프론트 페이지 구조 (`frontend/src/pages/`)

| 파일 | 경로 | 설명 |
|------|------|------|
| `Dashboard.jsx` | `/` | 메인 대시보드 |
| `Academic.jsx` | `/academic` | 성적·출결·졸업요건 |
| `Technical.jsx` | `/technical` | 포트폴리오·이력서·자기소개서 |
| `Career.jsx` | `/career` | 로드맵·취업준비·채용공고·자격증일정 |
| `Facilities.jsx` | `/facilities` | 시설 정보 |

`Layout.jsx` — SideNav(데스크탑) + TopNav + BottomNav(모바일) + AssistantPanel + SuggestionFab 포함.

TopNav 모바일: 햄버거 메뉴 → 우측 드로어 패널 (프로필/다크모드/설정/로그아웃).

---

## 알려진 이슈 / 미완성

1. **워크넷 UUID 키** — `openapi.work.go.kr`에서 "유효하지 않은 인증키" 반환. work24.go.kr 기업회원으로 재신청 필요. 엔드포인트는 `callOpenApiSvcInfo210L01.do`로 이미 수정됨.

2. **블라인드 채용 API** — `apis.data.go.kr/B490007/ncs.go.kr` 403. 별도 권한 신청 필요.

3. **Dashboard 하드코딩 (부분 해결)** — GPA·이수학점·출결률·결석경고는 실 API 연결 완료. 오늘의 강의·공지사항·시설 카드는 API 없어 더미 유지.

4. **사람인 API** — `oapi.saramin.co.kr` 신청 승인되면 `SaraminService` 추가 예정. 엔드포인트: `GET https://oapi.saramin.co.kr/job-search?access-key={키}&keywords={검색어}`.

5. **Q-Net 공개문제** — `apis.data.go.kr/B490007/openQst` 403. 별도 권한 필요.

---

## 회원가입 시 Student 자동 생성

`AuthService.register()` — `ROLE_STUDENT`로 가입 시 `Student` 엔티티를 자동 생성한다 (grade=1, semester=1, department="컴퓨터정보과"). 빠뜨리면 포트폴리오/이력서 저장 시 STUDENT_NOT_FOUND 500 발생.

---

## 변경 이력

### 2026-05-18
- **`config/AppConfig.java` 신규** — `RestTemplate` `@Bean` 정의 추가. 기존 코드에 누락되어 있어 `JobkoreaService` 등 5개 서비스 기동 실패하던 버그 수정.
- **`AssistantService.java` 버그 수정** — `GradWarning.earned` 가 `0`으로 하드코딩되던 문제 수정. `analyze()`에서 `earnedByName` 맵 계산 후 `parseResponse()`에 전달, 실제 이수학점·부족학점 반영.
- **`Dashboard.jsx` API 연결** — GPA·이수학점(`GET /api/grades/me`), 출석률·결석경고(`GET /api/attendance/me`) 실 데이터로 교체. `Promise.all` 병렬 호출, 로딩 중 `…` 표시.
- **개발 서버 구축** — `10.8.0.2`에 Docker Compose 환경 구성. 백엔드(8080) + 프론트(3000) 컨테이너, `network_mode: host`, `/home/ruddls030/campusflow` 배치.

---

## 자주 쓰는 패턴

```java
// 서비스에서 학생 조회
private Student getStudent(String username) {
    Long userId = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND))
            .getId();
    return studentRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
}
```

```java
// API 응답 래핑
return ApiResponse.ok(someData);  // { success: true, data: ... }
```

```js
// 프론트 API 호출 (axios 인터셉터가 JWT 자동 주입)
const res = await api.get('/some-endpoint')
const data = res.data.data  // ApiResponse.data 필드
```
