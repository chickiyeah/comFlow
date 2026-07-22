import api from './axios'

// 클래스 (NovaClass 포팅)
export const createClass = (data) => api.post('/classes', data)
export const getMyClasses = () => api.get('/classes')
export const joinClass = (code) => api.post('/classes/join', { code })
export const getClass = (classId) => api.get(`/classes/${classId}`)
export const getClassMembers = (classId) => api.get(`/classes/${classId}/members`)
export const inviteToClass = (classId, data) => api.post(`/classes/${classId}/invite`, data)
export const removeClassMember = (classId, userId) =>
  api.delete(`/classes/${classId}/members/${userId}`)
