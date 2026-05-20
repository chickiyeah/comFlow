import api from './axios'

export const getAllNotices = () => api.get('/notices')
export const getRecentNotices = (limit = 3) => api.get('/notices/recent', { params: { limit } })
export const createNotice = (data) => api.post('/notices', data)
export const deleteNotice = (id) => api.delete(`/notices/${id}`)
