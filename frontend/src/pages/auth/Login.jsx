import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../../api/auth'
import useAuthStore from '../../store/authStore'
import useThemeStore from '../../store/themeStore'

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore(s => s.setAuth)
  const { dark, toggle } = useThemeStore()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await login(form)
      setAuth(res.data.accessToken, { name: res.data.name, role: res.data.role })
      navigate('/')
    } catch (err) {
      setError(err?.message || '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background dark:bg-[#0b0e14] flex items-center justify-center p-4 transition-colors duration-300">

      {/* Dark mode toggle */}
      <button
        onClick={toggle}
        className="fixed top-4 right-4 p-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:scale-110 transition-transform"
      >
        <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
      </button>

      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary dark:bg-primary-container mb-4">
            <span className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">CF</span>
          </div>
          <h1 className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">CampusFlow</h1>
          <p className="text-on-surface-variant text-sm mt-1">컴퓨터정보과 학과 통합 관리 시스템</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 p-8 shadow-sm space-y-5">
          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">학번 / 교번</label>
            <input
              type="text"
              value={form.username}
              onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
              placeholder="학번 또는 교번 입력"
              required
            />
          </div>
          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">비밀번호</label>
            <input
              type="password"
              value={form.password}
              onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
              placeholder="비밀번호 입력"
              required
            />
          </div>

          {error && (
            <p className="text-error text-label-md bg-error-container dark:bg-error/20 px-3 py-2 rounded-lg">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-primary dark:bg-primary-container text-on-primary dark:text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-60"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
      </div>
    </div>
  )
}
