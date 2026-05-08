import api from './axios'

export const getCoverLetters = () => api.get('/cover-letters')
export const saveCoverLetter = (data) => api.post('/cover-letters', data)
export const updateCoverLetter = (id, data) => api.put(`/cover-letters/${id}`, data)
export const deleteCoverLetter = (id) => api.delete(`/cover-letters/${id}`)
