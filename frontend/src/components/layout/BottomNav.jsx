import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

const NAV_ITEMS = [
  { path: '/',           icon: 'dashboard',      key: 'dashboard'  },
  { path: '/academic',   icon: 'school',         key: 'academic'   },
  { path: '/facilities', icon: 'corporate_fare', key: 'facilities' },
  { path: '/technical',  icon: 'description',    key: 'technical'  },
  { path: '/career',     icon: 'work',           key: 'career'     },
]

export default function BottomNav() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { t } = useTranslation()

  return (
    <nav className="fixed bottom-0 left-0 w-full z-50 lg:hidden flex justify-around items-center
      bg-white/80 dark:bg-slate-950/80 backdrop-blur-md
      border-t border-slate-200 dark:border-slate-800
      shadow-[0_-4px_20px_-5px_rgba(30,58,138,0.08)] h-16 px-2">
      {NAV_ITEMS.map(({ path, icon, key }) => {
        const active = path === '/' ? pathname === '/' : pathname.startsWith(path)
        return (
          <button
            key={path}
            onClick={() => navigate(path)}
            className={
              active
                ? 'flex flex-col items-center justify-center bg-secondary-fixed dark:bg-secondary-fixed text-primary dark:text-[#0f172a] rounded-xl px-3 py-1 active:scale-90 transition-transform duration-150'
                : 'flex flex-col items-center justify-center text-slate-500 dark:text-slate-400 px-3 py-1 hover:text-blue-800 active:scale-90 transition-all duration-150'
            }
          >
            <span className={`material-symbols-outlined ${active ? 'icon-fill' : ''}`}>{icon}</span>
            <span className="font-['Space_Grotesk'] text-[10px] font-medium">{t(`nav.${key}`)}</span>
          </button>
        )
      })}
    </nav>
  )
}
