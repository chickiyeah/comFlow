import api from './axios'

export const getSessions         = () => api.get('/sessions')
export const revokeSession       = (id) => api.delete(`/sessions/${id}`)
export const revokeOtherSessions = () => api.delete('/sessions/others')
