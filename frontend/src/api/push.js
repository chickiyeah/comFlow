import api from './axios'

export const getPushPublicKey = () => api.get('/push/public-key')
export const subscribePush    = (sub) => api.post('/push/subscribe', sub)
export const unsubscribePush  = (endpoint) => api.post('/push/unsubscribe', { endpoint })
