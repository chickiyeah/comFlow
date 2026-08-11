import api from './axios'

// JobPilot — 채용 공고 → 직무 맞춤 자소서 파이프라인
// [수집] collect → [추출] extract → [매칭] match → [생성] generate

// 로그인 학생의 프로필 근거 (자동 조립, 검토 UI 에서 보정용)
export const getJobPilotProfile = () => api.get('/jobpilot/profile')

// [수집] URL 1건 on-demand fetch 또는 붙여넣기 → 공고 원문
export const collectJob = ({ url, text }) => api.post('/jobpilot/collect', { url, text })

// [추출] 공고 원문 → 구조화 JobPosting
export const extractJob = (text, url) => api.post('/jobpilot/extract', { text, url })

// [매칭] JobPosting(+프로필) → 강점/약점 리포트. profile 생략 시 서버가 조립
export const matchJob = (job, profile) => api.post('/jobpilot/match', { job, profile })

// [생성] JobPosting(+매칭+프로필) → 문항별 자소서 + 글자수 리포트
export const generateCoverLetter = (job, matchReport, profile) =>
  api.post('/jobpilot/generate', { job, matchReport, profile })

// [내보내기] 완성본을 기존 '내 자기소개서'(cover_letters)로 저장 — 회사별 보관
export const saveGeneratedCoverLetter = ({ title, companyName, jobTitle, content }) =>
  api.post('/cover-letters', { title, companyName, jobTitle, content })
