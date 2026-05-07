import api from './axios'

export const submitSuggestion = (data) => api.post('/suggestions', data)
