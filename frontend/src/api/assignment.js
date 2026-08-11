import api from './axios'

const multipart = { headers: { 'Content-Type': 'multipart/form-data' } }

// 과제
export const getAssignments = (classId) => api.get(`/classes/${classId}/assignments`)
export const createAssignment = (classId, data) => api.post(`/classes/${classId}/assignments`, data)
export const getAssignment = (assignmentId) => api.get(`/assignments/${assignmentId}`)
export const updateAssignment = (assignmentId, data) => api.put(`/assignments/${assignmentId}`, data)
export const setAssignmentDraft = (assignmentId, draft) =>
  api.patch(`/assignments/${assignmentId}/draft`, { draft })
export const setAssignmentTopic = (assignmentId, topic) =>
  api.patch(`/assignments/${assignmentId}/topic`, { topic })
export const deleteAssignment = (assignmentId) => api.delete(`/assignments/${assignmentId}`)
export const attachAssignmentFile = (assignmentId, file) => {
  const fd = new FormData()
  fd.append('file', file)
  return api.post(`/assignments/${assignmentId}/files`, fd, multipart)
}

// 제출
export const submitAssignment = (assignmentId, { content, file }) => {
  const fd = new FormData()
  if (content != null) fd.append('content', content)
  if (file) fd.append('file', file)
  return api.post(`/assignments/${assignmentId}/submit`, fd, multipart)
}
export const getSubmissions = (assignmentId) => api.get(`/assignments/${assignmentId}/submissions`)
export const getSubmissionStats = (assignmentId) =>
  api.get(`/assignments/${assignmentId}/submission-stats`)
export const gradeSubmission = (submissionId, { grade, feedback }) =>
  api.post(`/submissions/${submissionId}/grade`, { grade, feedback })
export const returnSubmission = (submissionId) => api.post(`/submissions/${submissionId}/return`)
export const aiCheckSubmission = (submissionId) => api.post(`/submissions/${submissionId}/ai-check`)

// 비공개 코멘트
export const getAssignmentComments = (assignmentId, studentId) =>
  api.get(`/assignments/${assignmentId}/comments`, { params: studentId ? { studentId } : {} })
export const addAssignmentComment = (assignmentId, { body, studentId }) =>
  api.post(`/assignments/${assignmentId}/comments`, { body, studentId })

// 성적부
export const getGradebook = (classId) => api.get(`/classes/${classId}/gradebook`)
