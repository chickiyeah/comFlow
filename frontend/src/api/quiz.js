import api from './axios'

export const getQuizzes      = (courseId) => api.get('/quizzes', { params: courseId ? { courseId } : {} })
export const getQuiz         = (id) => api.get(`/quizzes/${id}`)
export const createQuiz      = (data) => api.post('/quizzes', data)
export const generateQuiz    = (data) => api.post('/quizzes/generate', data)
export const updateQuiz      = (id, data) => api.put(`/quizzes/${id}`, data)
export const deleteQuiz      = (id) => api.delete(`/quizzes/${id}`)
export const submitQuiz      = (id, answers) => api.post(`/quizzes/${id}/submit`, { answers })
export const getMyQuizResult = (id) => api.get(`/quizzes/${id}/my-result`)
export const getQuizSubmissions = (id) => api.get(`/quizzes/${id}/submissions`)
export const overrideQuizAnswer = (answerId, data) => api.put(`/quizzes/answers/${answerId}`, data)
