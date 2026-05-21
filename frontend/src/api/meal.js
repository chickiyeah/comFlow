import api from './axios'

export const getTodayMeal = ()     => api.get('/meal/today')
export const getMeal      = (date) => api.get('/meal', { params: { date } })
