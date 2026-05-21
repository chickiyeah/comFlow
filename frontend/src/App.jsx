import { lazy, Suspense, useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import useAuthStore from './store/authStore'
import useThemeStore from './store/themeStore'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'

// 메인 페이지 — 라우트 단위 코드 스플리팅 (초기 번들 40%↓)
const Dashboard  = lazy(() => import('./pages/Dashboard'))
const Academic   = lazy(() => import('./pages/Academic'))
const Facilities = lazy(() => import('./pages/Facilities'))
const Career     = lazy(() => import('./pages/Career'))
const Technical  = lazy(() => import('./pages/Technical'))
const Calendar   = lazy(() => import('./pages/Calendar'))
const Study      = lazy(() => import('./pages/Study'))
const Profile    = lazy(() => import('./pages/Profile'))

function PrivateRoute({ children }) {
  const token = useAuthStore(s => s.token)
  return token ? children : <Navigate to="/login" replace />
}

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-white dark:bg-slate-950">
      <div className="w-8 h-8 border-4 border-primary border-t-secondary-fixed rounded-full animate-spin" />
    </div>
  )
}

export default function App() {
  const init = useThemeStore(s => s.init)
  useEffect(() => { init() }, [init])

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login"     element={<Login />} />
        <Route path="/register"  element={<Register />} />
        <Route path="/"          element={<PrivateRoute><Dashboard /></PrivateRoute>} />
        <Route path="/academic"  element={<PrivateRoute><Academic /></PrivateRoute>} />
        <Route path="/facilities"element={<PrivateRoute><Facilities /></PrivateRoute>} />
        <Route path="/career"    element={<PrivateRoute><Career /></PrivateRoute>} />
        <Route path="/technical" element={<PrivateRoute><Technical /></PrivateRoute>} />
        <Route path="/calendar"  element={<PrivateRoute><Calendar /></PrivateRoute>} />
        <Route path="/study"     element={<PrivateRoute><Study /></PrivateRoute>} />
        <Route path="/profile"   element={<PrivateRoute><Profile /></PrivateRoute>} />
      </Routes>
    </Suspense>
  )
}
