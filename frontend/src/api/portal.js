import api from './axios'

export const getPortalGradeTerms  = ()                         => api.get('/portal/grades/terms')
export const getPortalGradeDetail = (year, smr, meta = {})    => api.get('/portal/grades/detail', { params: { year, smr, ...meta } })
export const getPortalSchedule    = (year = '2026', smr = 'SU002001') => api.get('/portal/schedule', { params: { year, smr } })
export const getPortalShuttle     = ()                         => api.get('/portal/shuttle')
export const getPortalAttendance  = (schoolPassword, year = '2026', term = '1') =>
  api.post('/portal/attendance', { schoolPassword, year, term })
