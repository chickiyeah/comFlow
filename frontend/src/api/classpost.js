import api from './axios'

// 스트림 게시글
export const getPosts = (classId) => api.get(`/classes/${classId}/posts`)
export const createPost = (classId, data) => api.post(`/classes/${classId}/posts`, data)
export const updatePost = (postId, body) => api.put(`/posts/${postId}`, { body })
export const deletePost = (postId) => api.delete(`/posts/${postId}`)
export const addPostComment = (postId, body) => api.post(`/posts/${postId}/comments`, { body })
export const deletePostComment = (postId, commentId) =>
  api.delete(`/posts/${postId}/comments/${commentId}`)
