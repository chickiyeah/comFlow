import api from './axios'

export const listSessions   = ()                 => api.get('/chat/sessions')
export const createSession  = (message)          => api.post('/chat/sessions', { message })
export const getSession     = (id)               => api.get(`/chat/sessions/${id}`)
export const sendMessage    = (id, message)      => api.post(`/chat/sessions/${id}/messages`, { message })
export const renameSession  = (id, title)        => api.patch(`/chat/sessions/${id}`, { title })
export const deleteSession  = (id)               => api.delete(`/chat/sessions/${id}`)
