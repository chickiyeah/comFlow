import api from './axios'

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

// 자료
export const getMaterials = (classId) => api.get(`/classes/${classId}/materials`)
export const uploadMaterial = (classId, { title, instructions, week, topic, file }) => {
  const fd = new FormData()
  fd.append('title', title)
  if (instructions != null) fd.append('instructions', instructions)
  if (week != null) fd.append('week', week)
  if (topic != null) fd.append('topic', topic)
  if (file) fd.append('file', file)
  return api.post(`/classes/${classId}/materials`, fd, multipart)
}
export const getMaterial = (materialId) => api.get(`/materials/${materialId}`)
export const updateMaterial = (materialId, data) => api.put(`/materials/${materialId}`, data)
export const updateMaterialTopic = (materialId, topic) =>
  api.patch(`/materials/${materialId}/topic`, { topic })
export const deleteMaterial = (materialId) => api.delete(`/materials/${materialId}`)
export const getMaterialStreamTicket = (materialId) => api.post(`/materials/${materialId}/ticket`)

// 자료별 AI
export const getMaterialSummary = (materialId) => api.post(`/materials/${materialId}/summary`)
export const materialAi = (materialId, { action, message, level }) =>
  api.post(`/materials/${materialId}/ai`, { action, message, level })

// 북마크
export const getBookmarks = (materialId) => api.get(`/materials/${materialId}/bookmarks`)
export const addBookmark = (materialId, { page, note }) =>
  api.post(`/materials/${materialId}/bookmarks`, { page, note })
export const deleteBookmark = (materialId, page) =>
  api.delete(`/materials/${materialId}/bookmarks/${page}`)

// PDF 스마트 하이라이트
export const getHighlights = (materialId) => api.get(`/materials/${materialId}/highlights`)
export const startHighlights = (materialId, pages) =>
  api.post(`/materials/${materialId}/highlights/start`, { pages })
export const retryHighlights = (materialId) => api.post(`/materials/${materialId}/highlights/retry`)
