import api from './axios'

export const getMyAwards = () => api.get('/awards/me')
export const createAward = (data) => api.post('/awards', data)
export const deleteAward = (id) => api.delete(`/awards/${id}`)
