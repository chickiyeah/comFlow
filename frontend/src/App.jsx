import { lazy, Suspense, useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import useAuthStore from './store/authStore'
import useThemeStore from './store/themeStore'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import FindAccount from './pages/auth/FindAccount'

// 메인 페이지 — 라우트 단위 코드 스플리팅 (초기 번들 40%↓)
const Landing    = lazy(() => import('./pages/Landing'))
const Dashboard  = lazy(() => import('./pages/Dashboard'))
const Academic   = lazy(() => import('./pages/Academic'))
const Facilities = lazy(() => import('./pages/Facilities'))
const Career     = lazy(() => import('./pages/Career'))
const Interview  = lazy(() => import('./pages/Interview'))
const Technical  = lazy(() => import('./pages/Technical'))
const Calendar   = lazy(() => import('./pages/Calendar'))
const Study      = lazy(() => import('./pages/Study'))
const Classroom  = lazy(() => import('./pages/Classroom'))
const ClassDetail = lazy(() => import('./pages/ClassDetail'))
const KMate      = lazy(() => import('./pages/KMate'))
const Courses    = lazy(() => import('./pages/Courses'))
const Notices    = lazy(() => import('./pages/Notices'))
const Quizzes    = lazy(() => import('./pages/Quizzes'))
const Profile    = lazy(() => import('./pages/Profile'))
const Admin      = lazy(() => import('./pages/Admin'))
const Professor  = lazy(() => import('./pages/Professor'))

function PrivateRoute({ children }) {
  const token = useAuthStore(s => s.token)
  return token ? children : <Navigate to="/login" replace />
}

function AdminRoute({ children }) {
  const token = useAuthStore(s => s.token)
  const user  = useAuthStore(s => s.user)
  if (!token) return <Navigate to="/login" replace />
  if (user?.role !== 'ROLE_ADMIN') return <Navigate to="/" replace />
  return children
}

function ProfessorRoute({ children }) {
  const token = useAuthStore(s => s.token)
  const user  = useAuthStore(s => s.user)
  if (!token) return <Navigate to="/login" replace />
  if (user?.role !== 'ROLE_PROFESSOR' && user?.role !== 'ROLE_ADMIN') return <Navigate to="/" replace />
  return children
}

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-white dark:bg-slate-950">
      <div className="w-8 h-8 border-4 border-primary border-t-secondary-fixed rounded-full animate-spin" />
    </div>
  )
}

// 발표 덱 — 정적 HTML(public/presentation/index.html)을 전체화면 iframe으로
function Deck() {
  return (
    <iframe
      src="/presentation/index.html"
      title="CampusFlow 발표"
      style={{ position: 'fixed', inset: 0, width: '100%', height: '100%', border: 0 }}
    />
  )
}

// 알 수 없는 경로 처리 — 한글 /발표 는 여기서 디코딩해 덱으로 (윈도우 nginx 한글파일 이슈 우회)
function NotFound() {
  let p = '/'
  try { p = decodeURIComponent(window.location.pathname).replace(/\/+$/, '') } catch { /* ignore */ }
  if (p === '/발표' || p === '/presentation') return <Deck />
  return <Navigate to="/" replace />
}

export default function App() {
  const init = useThemeStore(s => s.init)
  useEffect(() => { init() }, [init])

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login"     element={<Login />} />
        <Route path="/register"  element={<Register />} />
        <Route path="/find-account" element={<FindAccount />} />
        <Route path="/"          element={<Landing />} />
        <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
        <Route path="/academic"  element={<PrivateRoute><Academic /></PrivateRoute>} />
        <Route path="/facilities"element={<PrivateRoute><Facilities /></PrivateRoute>} />
        <Route path="/career"    element={<PrivateRoute><Career /></PrivateRoute>} />
        <Route path="/interview" element={<PrivateRoute><Interview /></PrivateRoute>} />
        <Route path="/technical" element={<PrivateRoute><Technical /></PrivateRoute>} />
        <Route path="/calendar"  element={<PrivateRoute><Calendar /></PrivateRoute>} />
        <Route path="/study"     element={<PrivateRoute><Study /></PrivateRoute>} />
        <Route path="/classroom" element={<PrivateRoute><Classroom /></PrivateRoute>} />
        <Route path="/classroom/:id" element={<PrivateRoute><ClassDetail /></PrivateRoute>} />
        <Route path="/kmate"     element={<PrivateRoute><KMate /></PrivateRoute>} />
        <Route path="/courses"   element={<PrivateRoute><Courses /></PrivateRoute>} />
        <Route path="/notices"   element={<PrivateRoute><Notices /></PrivateRoute>} />
        <Route path="/quizzes"   element={<PrivateRoute><Quizzes /></PrivateRoute>} />
        <Route path="/profile"   element={<PrivateRoute><Profile /></PrivateRoute>} />
        <Route path="/admin"     element={<AdminRoute><Admin /></AdminRoute>} />
        <Route path="/professor" element={<ProfessorRoute><Professor /></ProfessorRoute>} />
        <Route path="/presentation" element={<Deck />} />
        <Route path="/발표"       element={<Deck />} />
        <Route path="*"          element={<NotFound />} />
      </Routes>
    </Suspense>
  )
}
