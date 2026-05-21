import api from './axios'

export const searchStudyGroups = (subject) => api.get('/study', { params: { subject } })
export const getMyStudyGroups  = ()        => api.get('/study/me')
export const createStudyGroup  = (data)    => api.post('/study', data)
export const joinStudyGroup    = (id)      => api.post(`/study/${id}/join`)
export const leaveStudyGroup   = (id)      => api.delete(`/study/${id}/leave`)
