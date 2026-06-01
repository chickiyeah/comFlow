import api from './axios'

export const getProfile        = ()                       => api.get('/profile/me')
export const syncProfile       = (schoolPassword, studentId) => api.post('/profile/sync', { schoolPassword, studentId })
export const disableSync       = ()                       => api.delete('/profile/sync')
export const updateAcademic    = (grade, semester)        => api.put('/profile/academic', { grade, semester })
