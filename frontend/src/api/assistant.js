import api from './axios'

export const analyzeStudent = () => api.get('/assistant/analyze')
