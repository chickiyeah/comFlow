import api from './axios'

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

// 클래스 자료실 (파일/링크)
export const getResources = (classId) => api.get(`/classes/${classId}/resources`)
export const createResource = (classId, { title, type, url, file }) => {
  const fd = new FormData()
  fd.append('title', title)
  if (type) fd.append('type', type)
  if (url != null) fd.append('url', url)
  if (file) fd.append('file', file)
  return api.post(`/classes/${classId}/resources`, fd, multipart)
}
export const deleteResource = (resourceId) => api.delete(`/resources/${resourceId}`)
