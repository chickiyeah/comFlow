import api from './axios'

export const generateStudyPlan = (data)  => api.post('/planner/generate', data)
export const predictGrade      = (data)  => api.post('/planner/predict-grade', data)
export const getMyWarnings     = ()      => api.get('/warning/me')
