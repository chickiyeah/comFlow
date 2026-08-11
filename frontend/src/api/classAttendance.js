import api from './axios'

// 클래스별 출석 (기존 /attendance 학과 출결과 별개)
export const getSessions = (classId) => api.get(`/classes/${classId}/attendance`)
export const createSession = (classId, data) => api.post(`/classes/${classId}/attendance`, data)
export const getMyClassAttendance = (classId) => api.get(`/classes/${classId}/attendance/me`)
export const getSession = (sessionId) => api.get(`/class-attendance/${sessionId}`)
export const markAttendance = (sessionId, { studentId, status }) =>
  api.patch(`/class-attendance/${sessionId}/mark`, { studentId, status })
export const deleteSession = (sessionId) => api.delete(`/class-attendance/${sessionId}`)
