import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import useAuthStore from '../../store/authStore'
import useThemeStore from '../../store/themeStore'

const LANGUAGES = [
  { code: 'ko', label: '한국어', flag: '🇰🇷' },
  { code: 'en', label: 'English', flag: '🇺🇸' },
  { code: 'zh', label: '中文',   flag: '🇨🇳' },
  { code: 'ja', label: '日本語', flag: '🇯🇵' },
  { code: 'vi', label: 'Tiếng Việt', flag: '🇻🇳' },
]

export default function TopNav({ title }) {
  const { user, logout } = useAuthStore()
  const { dark, toggle } = useThemeStore()
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()

  const [showMenu, setShowMenu] = useState(false)   // desktop avatar dropdown
  const [showLang, setShowLang] = useState(false)   // language dropdown
  const [showDrawer, setShowDrawer] = useState(false) // mobile drawer

  const currentLang = LANGUAGES.find(l => l.code === i18n.language) ?? LANGUAGES[0]

  const handleLogout = () => {
    logout()
    navigate('/login')
    setShowDrawer(false)
  }

  return (
    <>
      <header className="fixed top-0 left-0 lg:left-20 right-0 h-16 z-40
        bg-surface-container-lowest/90 dark:bg-[#12151c]/90
        backdrop-blur-md
        border-b border-outline-variant/40 dark:border-[#1e2a45]
        shadow-top flex justify-between items-center px-4 lg:px-6">

        {/* Left: logo + title */}
        <div className="flex items-center gap-4 min-w-0">
          <button onClick={() => navigate('/')}
            className="text-xl font-black tracking-tight text-primary dark:text-white font-['Space_Grotesk'] hover:opacity-80 transition-opacity active:scale-95 shrink-0">
            CampusFlow
          </button>
          <span className="text-on-surface-variant text-label-md bg-surface-container dark:bg-slate-800 px-3 py-1 rounded-full hidden sm:inline border border-outline-variant/30 shrink-0">
            2024년 1학기
          </span>
          {title && (
            <h1 className="text-lg font-bold text-primary dark:text-slate-300 border-l border-slate-200 dark:border-slate-800 pl-4 hidden md:block font-['Space_Grotesk'] truncate">
              {title}
            </h1>
          )}
        </div>

        {/* Right */}
        <div className="flex items-center gap-1 shrink-0">
          {/* Search — desktop only */}
          <div className="relative hidden lg:block group mr-2">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px] group-focus-within:text-primary transition-colors">
              search
            </span>
            <input
              className="pl-9 pr-4 py-2 bg-slate-100 dark:bg-slate-900 dark:border dark:border-slate-800 dark:text-on-surface border-none rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 dark:focus:ring-secondary-fixed/20 w-64 transition-all"
              placeholder={t('common.search')}
              type="text"
            />
          </div>

          {/* Language selector — always visible */}
          <div className="relative">
            <button
              onClick={() => setShowLang(v => !v)}
              className="flex items-center gap-1.5 px-2 lg:px-3 py-2 rounded-full text-sm font-medium text-on-surface-variant dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors active:scale-95"
            >
              <span>{currentLang.flag}</span>
              <span className="hidden sm:inline text-label-md">{currentLang.code.toUpperCase()}</span>
              <span className="material-symbols-outlined text-[14px]">expand_more</span>
            </button>
            {showLang && (
              <>
                <div className="fixed inset-0 z-40" onClick={() => setShowLang(false)} />
                <div className="absolute right-0 top-11 bg-white dark:bg-slate-900 rounded-xl shadow-lg border border-slate-100 dark:border-slate-800 py-1 w-44 z-50">
                  {LANGUAGES.map(lang => (
                    <button
                      key={lang.code}
                      onClick={() => { i18n.changeLanguage(lang.code); setShowLang(false) }}
                      className={`w-full text-left px-4 py-2.5 text-sm flex items-center gap-3 hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors ${
                        i18n.language === lang.code ? 'text-primary dark:text-secondary-fixed font-bold' : 'text-on-surface dark:text-slate-300'
                      }`}
                    >
                      <span>{lang.flag}</span>
                      <span>{lang.label}</span>
                      {i18n.language === lang.code && (
                        <span className="material-symbols-outlined text-[16px] ml-auto">check</span>
                      )}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>

          {/* Desktop-only buttons */}
          <button onClick={toggle}
            className="hidden lg:flex p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors active:scale-95"
            title={dark ? '라이트 모드' : '다크 모드'}>
            <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
          </button>

          <button className="hidden lg:flex p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors relative active:scale-95">
            <span className="material-symbols-outlined">notifications</span>
            <span className="absolute top-2 right-2 w-2 h-2 bg-error rounded-full ring-2 ring-white dark:ring-[#0a0c10]" />
          </button>

          <button className="hidden lg:flex p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors active:scale-95">
            <span className="material-symbols-outlined">settings</span>
          </button>

          {/* Desktop avatar dropdown */}
          <div className="relative hidden lg:block ml-1">
            <button
              onClick={() => setShowMenu(v => !v)}
              className="flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-800"
            >
              <div className="text-right">
                <p className="text-label-md font-bold text-primary dark:text-secondary-fixed">
                  {user?.name ?? '학생'} 학우님
                </p>
                <p className="text-[10px] text-on-surface-variant">컴퓨터정보과</p>
              </div>
              <div className="w-9 h-9 rounded-full bg-primary-container dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-secondary-fixed font-bold text-sm shrink-0">
                {user?.name?.[0] ?? 'S'}
              </div>
            </button>
            {showMenu && (
              <>
                <div className="fixed inset-0 z-40" onClick={() => setShowMenu(false)} />
                <div className="absolute right-0 top-12 bg-white dark:bg-slate-900 rounded-xl shadow-lg border border-slate-100 dark:border-slate-800 py-2 w-36 z-50">
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm text-error hover:bg-error-container dark:hover:bg-error/20 transition-colors flex items-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[16px]">logout</span>
                    로그아웃
                  </button>
                </div>
              </>
            )}
          </div>

          {/* Mobile hamburger */}
          <button
            onClick={() => setShowDrawer(true)}
            className="lg:hidden p-2 rounded-full text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors active:scale-95 ml-1"
          >
            <span className="material-symbols-outlined">menu</span>
          </button>
        </div>
      </header>

      {/* ── Mobile drawer ── */}
      {showDrawer && (
        <div className="fixed inset-0 z-50 lg:hidden">
          {/* backdrop */}
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => setShowDrawer(false)} />

          {/* panel */}
          <div className="absolute top-0 right-0 h-full w-72 bg-white dark:bg-slate-900 shadow-2xl flex flex-col">
            {/* Header */}
            <div className="flex items-center justify-between px-5 pt-5 pb-4 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-full bg-primary-container dark:bg-slate-800 border-2 border-primary/20 dark:border-secondary-fixed/20 flex items-center justify-center text-secondary-fixed font-black text-lg">
                  {user?.name?.[0] ?? 'S'}
                </div>
                <div>
                  <p className="font-bold text-primary dark:text-white text-sm">{user?.name ?? '학생'} 학우님</p>
                  <p className="text-[11px] text-on-surface-variant dark:text-slate-400">컴퓨터정보과</p>
                </div>
              </div>
              <button onClick={() => setShowDrawer(false)}
                className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>

            {/* Actions */}
            <div className="flex-1 py-4 px-3 space-y-1">
              <button onClick={() => { toggle(); }}
                className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-on-surface dark:text-slate-200">
                <span className="material-symbols-outlined text-slate-500 dark:text-slate-400">
                  {dark ? 'light_mode' : 'dark_mode'}
                </span>
                <span className="text-sm font-medium">{dark ? '라이트 모드' : '다크 모드'}</span>
                <span className={`ml-auto w-10 h-5 rounded-full transition-colors relative ${dark ? 'bg-primary dark:bg-secondary-fixed' : 'bg-slate-300 dark:bg-slate-600'}`}>
                  <span className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${dark ? 'translate-x-5' : 'translate-x-0.5'}`} />
                </span>
              </button>

              <button className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-on-surface dark:text-slate-200 relative">
                <span className="material-symbols-outlined text-slate-500 dark:text-slate-400">notifications</span>
                <span className="text-sm font-medium">알림</span>
                <span className="ml-auto w-2 h-2 bg-error rounded-full" />
              </button>

              <button className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-on-surface dark:text-slate-200">
                <span className="material-symbols-outlined text-slate-500 dark:text-slate-400">settings</span>
                <span className="text-sm font-medium">설정</span>
              </button>
            </div>

            {/* Logout */}
            <div className="px-3 pb-6 border-t border-slate-100 dark:border-slate-800 pt-3">
              <button
                onClick={handleLogout}
                className="w-full flex items-center gap-4 px-4 py-3 rounded-xl text-error hover:bg-error-container dark:hover:bg-error/20 transition-colors"
              >
                <span className="material-symbols-outlined">logout</span>
                <span className="text-sm font-semibold">로그아웃</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
