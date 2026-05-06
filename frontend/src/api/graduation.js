import api from './axios'

export const checkGraduation = () => api.get('/graduation/check')
