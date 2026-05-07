import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { login } from '../../api/auth'
import useAuthStore from '../../store/authStore'
import useThemeStore from '../../store/themeStore'

const LANGUAGES = [
  { code: 'ko', label: '한국어', flag: '🇰🇷' },
  { code: 'en', label: 'English', flag: '🇺🇸' },
  { code: 'zh', label: '中文',   flag: '🇨🇳' },
  { code: 'ja', label: '日本語', flag: '🇯🇵' },
  { code: 'vi', label: 'Tiếng Việt', flag: '🇻🇳' },
]

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore(s => s.setAuth)
  const { dark, toggle } = useThemeStore()
  const { t, i18n } = useTranslation()

  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showLang, setShowLang] = useState(false)

  const currentLang = LANGUAGES.find(l => l.code === i18n.language) ?? LANGUAGES[0]

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await login(form)
      setAuth(res.data.accessToken, { name: res.data.name, role: res.data.role })
      navigate('/')
    } catch (err) {
      setError(err?.message || t('auth.loginFailed'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background dark:bg-[#0b0e14] flex items-center justify-center p-4 transition-colors duration-300">

      {/* Top-right controls */}
      <div className="fixed top-4 right-4 flex items-center gap-2">

        {/* Language selector */}
        <div className="relative">
          <button
            onClick={() => setShowLang(v => !v)}
            className="flex items-center gap-1.5 px-3 py-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:scale-105 transition-transform text-sm font-medium"
          >
            <span>{currentLang.flag}</span>
            <span>{currentLang.code.toUpperCase()}</span>
            <span className="material-symbols-outlined text-[14px]">expand_more</span>
          </button>

          {showLang && (
            <div className="absolute right-0 top-11 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-100 dark:border-slate-800 py-1.5 w-44 z-50 overflow-hidden">
              {LANGUAGES.map(lang => (
                <button
                  key={lang.code}
                  onClick={() => { i18n.changeLanguage(lang.code); setShowLang(false) }}
                  className={`w-full text-left px-4 py-2.5 text-sm flex items-center gap-3 hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors ${
                    i18n.language === lang.code
                      ? 'text-primary dark:text-secondary-fixed font-bold'
                      : 'text-on-surface dark:text-slate-300'
                  }`}
                >
                  <span className="text-base">{lang.flag}</span>
                  <span>{lang.label}</span>
                  {i18n.language === lang.code && (
                    <span className="material-symbols-outlined text-[16px] ml-auto">check</span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Dark mode toggle */}
        <button
          onClick={toggle}
          className="p-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:scale-110 transition-transform"
        >
          <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
        </button>
      </div>

      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary dark:bg-primary-container mb-4">
            <span className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">CF</span>
          </div>
          <h1 className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">CampusFlow</h1>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">컴퓨터정보과 학과 통합 관리 시스템</p>
        </div>

        {/* Form */}
        <form
          onSubmit={handleSubmit}
          className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 p-8 shadow-sm space-y-5"
        >
          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">
              {t('auth.studentId')}
            </label>
            <input
              type="text"
              value={form.username}
              onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
              placeholder={t('auth.studentId')}
              required
            />
          </div>

          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">
              {t('auth.password')}
            </label>
            <input
              type="password"
              value={form.password}
              onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
              placeholder={t('auth.password')}
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
            {loading ? t('auth.loggingIn') : t('auth.loginBtn')}
          </button>
        </form>

        <p className="text-center text-sm text-on-surface-variant dark:text-slate-400 mt-6">
          계정이 없으신가요?{' '}
          <Link to="/register" className="text-primary dark:text-secondary-fixed font-bold hover:underline">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  )
}
