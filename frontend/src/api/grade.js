import api from './axios'

export const getMyGrades = () => api.get('/grades/me')
export const getGradesBySemester = (year, semester) =>
  api.get('/grades/me/semester', { params: { year, semester } })
