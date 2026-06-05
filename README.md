# CampusFlow

전주비전대 컴퓨터정보과 캡스톤 — 학생의 **성적·출결·포트폴리오·자격증·취업 준비를 AI와 연계해 통합 관리**하는 학과 시스템. 학업 현황 파악 → AI 어드바이저 분석 → 취업 준비(채용공고·자격증·AI 자소서)까지 원스톱.

## 기술 스택
- **Backend**: Spring Boot 3.3.5, Java 17, Spring Data JPA, Spring Security + JWT
- **Frontend**: React 18, Vite, Tailwind CSS, react-i18next(5개 언어)
- **DB**: MySQL 8
- **AI**: 폴백 체인(자체 vLLM → Ollama `qwen3` → Gemini) + 사내 RAG(컴정이, OpenAI 호환)
- **Cache**: Caffeine + Spring Cache
- **문서**: Apache POI(PPTX 추출), openhtmltopdf(HTML→PDF)

## 주요 기능
- **학사**: 성적·출결·시간표·졸업요건·통합 캘린더·학습 분석·조기경보·학식·시설·도서관
- **취업/포트폴리오**: 포트폴리오·이력서·자기소개서·수상내역·채용 알리미·채용공고 검색(Q-Net/Jobkorea/Work24/Worknet/Saramin)
- **학습/소셜**: 스터디 매칭·강의 리뷰
- **AI**: 어드바이저 분석·AI 자소서·공부 플래너·성적 예측·커리어 로드맵
- **학교 포털 연동**: 성적·시간표·셔틀·출결(check.jvision + LMS) 실데이터

## 아키텍처
Spring Boot REST API + React SPA. 학교 포털/LMS 실데이터는 별도 Python 연동 서비스가 수집 → `/api/portal/*`로 중계. AI는 폴백 체인 + 컴정이 RAG.

## 실행
**개발 (Docker Compose)**
```
docker compose up -d --build
```
backend `:8080` / frontend `:3000`

**운영 (Windows)** — Spring Boot JAR을 NSSM 서비스로, 프론트(Vite 빌드)는 nginx가 서빙 + `/api/` 프록시

**자동 배포** — `./deploy.sh [all|backend|frontend]`

## 환경 변수 (`.env`)
DB 접속 정보, JWT 시크릿, AI 설정(자체 vLLM 토큰·Ollama 서버 목록·Gemini 키·컴정이 base URL), 외부 채용/자격증 API 키, SMTP 메일. 실제 값은 커밋 금지 — `.env`는 `.gitignore` 대상.

## 디렉터리
- `src/main` — Spring Boot 백엔드 (`com.campusflow`)
- `frontend/` — React + Vite
- `*.py` (루트) — 학교 포털/LMS 연동 스크립트
- `Dockerfile.backend`, `Dockerfile.frontend`, `docker-compose.yml`, `nginx.conf`, `deploy.sh`
