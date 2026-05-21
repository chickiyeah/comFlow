import api from './axios'

export const getCalendarEvents = (year, month) =>
  api.get('/calendar/events', { params: { year, month } })
