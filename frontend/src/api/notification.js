import api from './axios'

export const getNotifications        = () => api.get('/notifications')
export const getUnreadCount          = () => api.get('/notifications/unread-count')
export const markNotificationRead    = (id) => api.put(`/notifications/${id}/read`)
export const markAllNotificationsRead = () => api.put('/notifications/read-all')
export const refreshJobNotifications = () => api.post('/notifications/refresh-jobs')
export const getNotifPrefs    = () => api.get('/notifications/prefs')
export const updateNotifPrefs = (data) => api.put('/notifications/prefs', data)
