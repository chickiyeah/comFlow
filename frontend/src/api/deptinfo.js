import api from './axios'

export const getDeptInfos   = () => api.get('/admin/dept-info')
export const createDeptInfo = (data) => api.post('/admin/dept-info', data)
export const updateDeptInfo = (id, data) => api.put(`/admin/dept-info/${id}`, data)
export const deleteDeptInfo = (id) => api.delete(`/admin/dept-info/${id}`)
