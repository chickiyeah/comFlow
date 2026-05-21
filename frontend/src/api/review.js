import api from './axios'

export const getReviewsBySubject = (subject) => api.get('/reviews', { params: { subject } })
export const getMyReviews        = ()         => api.get('/reviews/me')
export const createReview        = (data)     => api.post('/reviews', data)
export const deleteReview        = (id)       => api.delete(`/reviews/${id}`)
