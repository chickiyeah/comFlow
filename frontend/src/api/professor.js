import api from './axios'

export const getProfOverview      = ()   => api.get('/professor/overview')
export const getProfStudents      = ()   => api.get('/professor/students')
export const getProfStudentDetail = (id) => api.get(`/professor/students/${id}`)
export const notifyProfStudent    = (id, data) => api.post(`/professor/students/${id}/notify`, data)
export const notifyProfAtRisk     = (data) => api.post('/professor/notify-at-risk', data)
export const getProfAnalytics     = ()     => api.get('/professor/analytics')
