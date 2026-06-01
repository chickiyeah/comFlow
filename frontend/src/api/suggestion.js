import api from './axios'

export const submitSuggestion = (data) => api.post('/suggestions', data)

// ADMIN 전용
export const getAllSuggestions = (page = 0, size = 20) =>
  api.get('/suggestions', { params: { page, size } })

export const replySuggestion = (id, reply, status) =>
  api.patch(`/suggestions/${id}/reply`, null, { params: { reply, status } })
