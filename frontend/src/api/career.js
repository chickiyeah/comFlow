import api from './axios'

// Career Activities
export const getActivities = () => api.get('/career/activities')
export const getActivitySummary = () => api.get('/career/activities/summary')
export const createActivity = (data) => api.post('/career/activities', data)
export const updateActivity = (id, data) => api.put(`/career/activities/${id}`, data)
export const deleteActivity = (id) => api.delete(`/career/activities/${id}`)

// Saved Jobs
export const getSavedJobs = () => api.get('/career/saved-jobs')
export const saveJob = (data) => api.post('/career/saved-jobs', data)
export const deleteSavedJob = (id) => api.delete(`/career/saved-jobs/${id}`)

// Job Search (Worknet / Q-Net)
export const searchJobs = (keyword = 'IT', page = 1) =>
  api.get('/career/search/jobs', { params: { keyword, page } })
export const searchCerts = (keyword, year) =>
  api.get('/career/search/certs', { params: { keyword, year } })

// GitHub Token
export const getGithubTokenStatus = () => api.get('/user/github-token')
export const saveGithubToken = (token) => api.post('/user/github-token', { token })
export const deleteGithubToken = () => api.delete('/user/github-token')
