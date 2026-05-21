import api from './axios'

export const getProfile      = ()           => api.get('/profile/me')
export const syncProfile     = (schoolPassword) => api.post('/profile/sync', { schoolPassword })
export const disableSync     = ()           => api.delete('/profile/sync')
