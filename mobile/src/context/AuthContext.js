import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { login as loginApi } from '../api/endpoints';
import { saveToken, getToken, deleteToken } from '../lib/storage';
import { setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // 앱 시작 시 저장된 토큰 복원
  useEffect(() => {
    (async () => {
      try {
        const t = await getToken();
        setToken(t || null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const signOut = useCallback(async () => {
    await deleteToken();
    setToken(null);
  }, []);

  // 401 인터셉터에서 토큰 만료 시 자동 로그아웃
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setToken(null);
    });
  }, []);

  const signIn = useCallback(async (username, password) => {
    const data = await loginApi(username, password);
    const accessToken = data?.accessToken || data?.token;
    if (!accessToken) {
      throw new Error('로그인 응답에 토큰이 없습니다.');
    }
    await saveToken(accessToken);
    setToken(accessToken);
    return accessToken;
  }, []);

  return (
    <AuthContext.Provider value={{ token, loading, signIn, signOut, isAuthed: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
