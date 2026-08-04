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

// 채용공고 통합 검색 (filters.hideExpired=true 면 마감 지난 공고 제외, source='imported' 면 적재본)
export const searchJobs = (keyword = 'IT', page = 0, filters = {}) =>
  api.get('/career/search/jobs', {
    params: { keyword, page, ...filters }
  })

// 공공 채용공고(고용24) 수동 수집 트리거 — 백그라운드 적재 즉시 갱신
export const refreshImportedJobs = () => api.post('/career/search/imported-jobs/refresh')

// 개인별 취업 통계 (희망 직무 기반) — jobTitle 생략 시 저장된 희망직무 사용
export const getJobStatistics = (jobTitle) =>
  api.get('/career/statistics', { params: jobTitle ? { jobTitle } : {} })

// Q-Net 자격증 API
export const getCertSchedules = (keyword, year) =>
  api.get('/career/search/certs/schedules', { params: { keyword, year } })
export const searchQualifications = (keyword) =>
  api.get('/career/search/certs/list', { params: { keyword } })
export const getQualificationDetail = (jmCd, qualgbCd) =>
  api.get('/career/search/certs/detail', { params: { jmCd, qualgbCd } })
export const getExamLocations = (brchCd) =>
  api.get('/career/search/certs/locations', { params: { brchCd } })

// 블라인드 채용 기업
export const searchBlindRecruit = (keyword, page = 1) =>
  api.get('/career/search/blind-recruit', { params: { keyword, page } })

// 프로필 기반 검색 키워드 추천 (기본 키워드 + "이 직무는 어때요?" 칩)
export const getKeywordSuggestions = () => api.get('/career/search/keyword-suggestions')

// GitHub Token
export const getGithubTokenStatus = () => api.get('/user/github-token')
export const saveGithubToken = (token) => api.post('/user/github-token', { token })
export const deleteGithubToken = () => api.delete('/user/github-token')
