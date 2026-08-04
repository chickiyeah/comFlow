import api from './axios'

export const getMyGamification = () => api.get('/gamification/me')
