import api from './axios'

export const analyzeStudent = () => api.get('/assistant/analyze')
export const generateCoverLetter = (data) => api.post('/assistant/cover-letter', data)
