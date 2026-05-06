import api from './axios'

export const getResumes = () => api.get('/resumes')
export const getResume = (id) => api.get(`/resumes/${id}`)
export const createResume = (data) => api.post('/resumes', data)
export const updateResume = (id, data) => api.put(`/resumes/${id}`, data)
export const deleteResume = (id) => api.delete(`/resumes/${id}`)
export const downloadResumePdf = (id) =>
  api.get(`/resumes/${id}/pdf`, { responseType: 'blob' })
