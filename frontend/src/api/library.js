import api from './axios'

export const searchBooks = (keyword, category, page = 0, size = 20) =>
  api.get('/books/search', { params: { keyword, category, page, size } })
