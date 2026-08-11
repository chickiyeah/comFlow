import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('campusflow-auth')
  if (stored) {
    const { state } = JSON.parse(stored)
    if (state?.token) {
      config.headers.Authorization = `Bearer ${state.token}`
    }
  }
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    // 로그인/회원가입 등 auth 요청의 401(자격증명 오류)은 새로고침하지 않고
    // 컴포넌트가 에러 메시지를 표시하도록 그대로 반려한다.
    const url = err.config?.url || ''
    const isAuthRequest = url.includes('/auth/')
    if (err.response?.status === 401 && !isAuthRequest) {
      // 인증된 요청의 토큰 만료 → 로그인으로
      localStorage.removeItem('campusflow-auth')
      window.location.href = '/login'
    }
    return Promise.reject(err.response?.data || err)
  }
)

export default api
