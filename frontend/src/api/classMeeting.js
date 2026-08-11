import api from './axios'

// 클래스 화상 미팅 (Jitsi)
export const startMeeting = (classId) => api.post(`/classes/${classId}/meeting`)
export const getMeeting = (classId) => api.get(`/classes/${classId}/meeting`)
export const endMeeting = (classId) => api.delete(`/classes/${classId}/meeting`)
