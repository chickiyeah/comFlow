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

// 채용공고 통합 검색
export const searchJobs = (keyword = 'IT', page = 0, filters = {}) =>
  api.get('/career/search/jobs', {
    params: { keyword, page, ...filters }
  })

// Q-Net 자격증 API
export const getCertSchedules = (keyword, year) =>
  api.get('/career/search/certs/schedules', { params: { keyword, year } })
export const searchQualifications = (keyword) =>
  api.get('/career/search/certs/list', { params: { keyword } })
export const getQualificationDetail = (jmCd, qualgbCd) =>
  api.get('/career/search/certs/detail', { params: { jmCd, qualgbCd } })
export const getPracticeQuestions = (keyword, year) =>
  api.get('/career/search/certs/questions', { params: { keyword, year } })
export const getExamLocations = (brchCd) =>
  api.get('/career/search/certs/locations', { params: { brchCd } })

// 블라인드 채용 기업
export const searchBlindRecruit = (keyword, page = 1) =>
  api.get('/career/search/blind-recruit', { params: { keyword, page } })

// GitHub Token
export const getGithubTokenStatus = () => api.get('/user/github-token')
export const saveGithubToken = (token) => api.post('/user/github-token', { token })
export const deleteGithubToken = () => api.delete('/user/github-token')
