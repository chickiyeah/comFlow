import api from './axios'

export const getPortfolios = () => api.get('/portfolios')
export const createPortfolio = (data) => api.post('/portfolios', data)
export const updatePortfolio = (id, data) => api.put(`/portfolios/${id}`, data)
export const deletePortfolio = (id) => api.delete(`/portfolios/${id}`)
export const generateFromGithub = (githubUrl) =>
  api.post('/portfolios/generate/github', { githubUrl })
export const generateFromFile = (file) => {
  const form = new FormData()
  form.append('file', file)
  return api.post('/portfolios/generate/file', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
