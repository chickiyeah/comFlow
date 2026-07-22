import api from './axios'

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

// 멀티모달 분석 (문서/음성/이미지)
export const analyzeFile = (file, question) => {
  const fd = new FormData()
  fd.append('file', file)
  if (question != null) fd.append('question', question)
  return api.post('/multimodal/analyze', fd, multipart)
}
