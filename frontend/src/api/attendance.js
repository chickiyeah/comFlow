import api from './axios'

export const getMyAttendance = () => api.get('/attendance/me')
