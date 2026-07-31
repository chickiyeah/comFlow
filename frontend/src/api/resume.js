import api from './axios'

export const getResumes = () => api.get('/resumes')
export const getResume = (id) => api.get(`/resumes/${id}`)
export const createResume = (data) => api.post('/resumes', data)
export const updateResume = (id, data) => api.put(`/resumes/${id}`, data)
export const deleteResume = (id) => api.delete(`/resumes/${id}`)
export const downloadResumePdf = (id) =>
  api.get(`/resumes/${id}/pdf`, { responseType: 'blob' })

// AI 이력서 초안 생성 (저장 안 함) — { data: ResumeData, honestyReport, template }
export const generateResume = (template = 'general') =>
  api.post(`/resumes/generate?template=${encodeURIComponent(template)}`)
