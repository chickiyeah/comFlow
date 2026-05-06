import api from './axios'

export const generateRoadmap = (jobTitle, useExternalAi = true) =>
  api.post('/roadmap/generate', { jobTitle, useExternalAi })
