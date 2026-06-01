import api from './axios'

export const getFacilityStats = () => api.get('/facilities/stats')
export const updateFacilityStat = (key, value) => api.put(`/facilities/stats/${key}`, { value })
