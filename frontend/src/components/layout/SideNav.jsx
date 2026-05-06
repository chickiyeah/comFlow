import { useLocation, useNavigate } from 'react-router-dom'

const navItems = [
  { path: '/',           icon: 'dashboard',      label: 'Dashboard'  },
  { path: '/academic',   icon: 'school',         label: 'Academic'   },
  { path: '/facilities', icon: 'corporate_fare', label: 'Facilities' },
  { path: '/career',     icon: 'work',           label: 'Career'     },
  { path: '/technical',  icon: 'description',    label: 'Technical'  },
]

export default function SideNav() {
  const { pathname } = useLocation()
  const navigate = useNavigate()

  return (
    <aside className="fixed left-0 top-0 h-full w-20 flex flex-col items-center py-4 z-50
      bg-blue-900 dark:bg-[#0f172a]
      border-r border-blue-800 dark:border-slate-800
      shadow-[4px_0_24px_rgba(30,58,138,0.08)] dark:shadow-[4px_0_24px_rgba(0,0,0,0.5)]">

      {/* Logo */}
      <div className="flex flex-col items-center mb-8">
        <span className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">CF</span>
        <span className="text-blue-200/50 dark:text-slate-500 text-[8px] font-bold tracking-wider font-['Space_Grotesk']">v1.0</span>
      </div>

      {/* Nav */}
      <nav className="flex flex-col items-center flex-1 space-y-6 w-full">
        {navItems.map(({ path, icon, label }) => {
          const active = path === '/' ? pathname === '/' : pathname.startsWith(path)
          return (
            <button
              key={path}
              onClick={() => navigate(path)}
              className={
                active
                  ? 'flex flex-col items-center justify-center w-14 h-14 bg-secondary-fixed text-primary dark:text-[#0f172a] rounded-xl shadow-[0_0_15px_rgba(190,242,100,0.4)] dark:shadow-[0_0_15px_rgba(190,242,100,0.3)] transition-transform active:scale-95'
                  : 'group flex flex-col items-center justify-center text-blue-200/70 dark:text-slate-400 hover:text-white transition-all duration-300 w-full py-2'
              }
            >
              <span className="material-symbols-outlined text-[22px] group-hover:scale-110 transition-transform">
                {icon}
              </span>
              <span className="font-['Space_Grotesk'] text-[8px] uppercase tracking-wider font-medium mt-1">
                {label}
              </span>
            </button>
          )
        })}
      </nav>

      {/* Support */}
      <div className="mt-auto">
        <button className="group flex flex-col items-center text-blue-200/70 dark:text-slate-400 hover:text-white transition-all duration-300 py-2 w-full">
          <span className="material-symbols-outlined text-[22px] group-hover:scale-110 transition-transform">help_outline</span>
          <span className="font-['Space_Grotesk'] text-[8px] uppercase tracking-wider font-medium mt-1">Support</span>
        </button>
      </div>
    </aside>
  )
}
