import api from './axios'

export const askKomjeong = (query, sessionId = '') =>
  api.post('/komjeong/chat', { query, sessionId })
