import api from './axios'

export const getGradeTrend       = () => api.get('/analytics/grades')
export const getAttendanceSummary = () => api.get('/analytics/attendance')
