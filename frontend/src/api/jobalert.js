import api from './axios'

export const getMyAlerts    = ()           => api.get('/career/alerts')
export const createAlert    = (data)       => api.post('/career/alerts', data)
export const deleteAlert    = (id)         => api.delete(`/career/alerts/${id}`)
