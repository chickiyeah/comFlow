import api from './axios'

// K.MATE — TOPIK AI 튜터
export const askKmate = (question) => api.post('/kmate/ask', { question })
export const getKmateHistory = () => api.get('/kmate/history')
export const generateKmateQuiz = ({ topic, count, language }) =>
  api.post('/kmate/quiz/generate', { topic, count, language })
export const checkKmateQuiz = (items) => api.post('/kmate/quiz/check', { items })
