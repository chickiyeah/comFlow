import api from './axios'

export const login = (data) => api.post('/auth/login', data)
export const logout = () => api.post('/auth/logout')
export const register = (data) => api.post('/auth/register', data)
export const sendEmailCode = (email) => api.post('/auth/email/send', { email })
export const verifyEmailCode = (email, code) => api.post('/auth/email/verify', { email, code })
export const findUsername = (email) => api.post('/auth/find-username', { email })
export const resetPassword = (email, code, newPassword) => api.post('/auth/reset-password', { email, code, newPassword })
