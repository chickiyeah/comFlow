import api from './axios'

// 클래스룸 대시보드 요약
export const getProgressSummary = () => api.get('/progress/summary')
