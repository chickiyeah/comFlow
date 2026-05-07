import { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import useAuthStore from './store/authStore'
import useThemeStore from './store/themeStore'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import Dashboard from './pages/Dashboard'
import Academic from './pages/Academic'
import Facilities from './pages/Facilities'
import Career from './pages/Career'
import Technical from './pages/Technical'

function PrivateRoute({ children }) {
  const token = useAuthStore(s => s.token)
  return token ? children : <Navigate to="/login" replace />
}

export default function App() {
  const init = useThemeStore(s => s.init)
  useEffect(() => { init() }, [init])

  return (
    <Routes>
      <Route path="/login"    element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/"          element={<PrivateRoute><Dashboard /></PrivateRoute>} />
      <Route path="/academic"  element={<PrivateRoute><Academic /></PrivateRoute>} />
      <Route path="/facilities"element={<PrivateRoute><Facilities /></PrivateRoute>} />
      <Route path="/career"    element={<PrivateRoute><Career /></PrivateRoute>} />
      <Route path="/technical" element={<PrivateRoute><Technical /></PrivateRoute>} />
    </Routes>
  )
}
