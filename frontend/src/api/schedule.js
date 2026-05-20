import api from './axios'

export const getTodaySchedule = () => api.get('/schedule/me/today')
export const getAllSchedule = () => api.get('/schedule/me')
export const createSchedule = (data) => api.post('/schedule', data)
export const updateSchedule = (id, data) => api.put(`/schedule/${id}`, data)
export const deleteSchedule = (id) => api.delete(`/schedule/${id}`)
