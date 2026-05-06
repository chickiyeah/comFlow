import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import useThemeStore from '../../store/themeStore'

export default function TopNav({ title }) {
  const { user, logout } = useAuthStore()
  const { dark, toggle } = useThemeStore()
  const navigate = useNavigate()
  const [showMenu, setShowMenu] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="fixed top-0 right-0 w-[calc(100%-80px)] h-16 z-40
      bg-white/90 dark:bg-[#0a0c10]/80
      backdrop-blur-md
      border-b border-slate-200/50 dark:border-slate-800
      shadow-sm flex justify-between items-center px-6">

      <div className="flex items-center gap-6">
        <div className="flex items-center gap-2">
          <span className="text-xl font-black tracking-tight text-primary dark:text-white font-['Space_Grotesk']">
            CampusFlow
          </span>
          <span className="text-on-surface-variant text-label-md bg-surface-container dark:bg-slate-800 px-3 py-1 rounded-full hidden sm:inline border border-outline-variant/30">
            2024년 1학기
          </span>
        </div>
        {title && (
          <h1 className="text-lg font-bold text-primary dark:text-slate-300 border-l border-slate-200 dark:border-slate-800 pl-6 hidden md:block font-['Space_Grotesk']">
            {title}
          </h1>
        )}
      </div>

      <div className="flex items-center gap-1">
        {/* Search */}
        <div className="relative hidden lg:block group mr-2">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[18px] group-focus-within:text-primary transition-colors">
            search
          </span>
          <input
            className="pl-9 pr-4 py-2 bg-slate-100 dark:bg-slate-900 dark:border dark:border-slate-800 dark:text-on-surface border-none rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 dark:focus:ring-secondary-fixed/20 w-64 transition-all"
            placeholder="검색..."
            type="text"
          />
        </div>

        {/* Dark mode toggle */}
        <button
          onClick={toggle}
          className="p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors active:scale-95"
          title={dark ? '라이트 모드' : '다크 모드'}
        >
          <span className="material-symbols-outlined">
            {dark ? 'light_mode' : 'dark_mode'}
          </span>
        </button>

        {/* Notifications */}
        <button className="p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors relative active:scale-95">
          <span className="material-symbols-outlined">notifications</span>
          <span className="absolute top-2 right-2 w-2 h-2 bg-error rounded-full ring-2 ring-white dark:ring-[#0a0c10]" />
        </button>

        {/* Settings */}
        <button className="p-2 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors active:scale-95">
          <span className="material-symbols-outlined">settings</span>
        </button>

        {/* Avatar + dropdown */}
        <div className="relative ml-1">
          <button
            onClick={() => setShowMenu(v => !v)}
            className="flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-800"
          >
            <div className="text-right hidden sm:block">
              <p className="text-label-md font-bold text-primary dark:text-secondary-fixed">
                {user?.name ?? '학생'} 학우님
              </p>
              <p className="text-[10px] text-on-surface-variant">컴퓨터정보과</p>
            </div>
            <div className="w-9 h-9 rounded-full bg-primary-container dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center text-secondary-fixed font-bold text-sm">
              {user?.name?.[0] ?? 'S'}
            </div>
          </button>

          {showMenu && (
            <div className="absolute right-0 top-12 bg-white dark:bg-slate-900 rounded-xl shadow-lg border border-slate-100 dark:border-slate-800 py-2 w-36 z-50">
              <button
                onClick={handleLogout}
                className="w-full text-left px-4 py-2 text-sm text-error hover:bg-error-container dark:hover:bg-error/20 transition-colors flex items-center gap-2"
              >
                <span className="material-symbols-outlined text-[16px]">logout</span>
                로그아웃
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
