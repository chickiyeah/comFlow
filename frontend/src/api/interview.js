import api from './axios'

/** 가상 면접 세션 시작 */
export const startInterview = (data) =>
  api.post('/interview/sessions', data)

/** 답변 제출 → 피드백 + 다음 질문 */
export const submitAnswer = (sessionId, answer) =>
  api.post(`/interview/sessions/${sessionId}/answer`, { answer })

/** 내 면접 세션 목록 */
export const listSessions = () =>
  api.get('/interview/sessions')

/** 세션 상세 (결과) */
export const getSessionDetail = (sessionId) =>
  api.get(`/interview/sessions/${sessionId}`)
