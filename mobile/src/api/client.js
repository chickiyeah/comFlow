import axios from 'axios';
import { API_BASE_URL } from '../config/env';
import { getToken, deleteToken } from '../lib/storage';

// 인증 만료 시 호출될 콜백 (AuthContext 에서 등록)
let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// 요청마다 저장된 JWT 를 Authorization 헤더로 자동 첨부
api.interceptors.request.use(async (config) => {
  const token = await getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const url = err.config?.url || '';
    const isAuthRequest = url.includes('/auth/');
    if (err.response?.status === 401 && !isAuthRequest) {
      await deleteToken();
      if (onUnauthorized) onUnauthorized();
    }
    return Promise.reject(err);
  }
);

// 백엔드 응답은 { success, data } 래핑 → data 를 꺼내 반환하는 헬퍼
export function unwrap(res) {
  return res?.data?.data ?? res?.data;
}

export default api;
