import { useState, useRef, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import useAuthStore from '../../store/authStore'
import { NAV_CATEGORIES, roleGatedItems, containsPath } from '../../config/navConfig'

// 데스크탑 사이드 레일. 대메뉴(rail 아이콘) 클릭 시 자식이 있으면 중/소메뉴 플라이아웃을 연다.
export default function SideNav() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const user = useAuthStore(s => s.user)

  const [openKey, setOpenKey] = useState(null)
  const [flyoutTop, setFlyoutTop] = useState(80)
  const btnRefs = useRef({})
  const panelRef = useRef(null)

  const items = [...NAV_CATEGORIES, ...roleGatedItems(user?.role)]
  const openItem = items.find(i => i.key === openKey)

  // 라우트 이동 시 플라이아웃 자동 닫기
  useEffect(() => { setOpenKey(null) }, [pathname])

  // 바깥 클릭 시 닫기
  useEffect(() => {
    if (!openKey) return
    const handler = (e) => {
      if (panelRef.current?.contains(e.target)) return
      if (btnRefs.current[openKey]?.contains(e.target)) return
      setOpenKey(null)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [openKey])

  const handleClick = (item) => {
    if (!item.children) {
      navigate(item.path)
      return
    }
    if (openKey === item.key) {
      setOpenKey(null)
      return
    }
    const rect = btnRefs.current[item.key]?.getBoundingClientRect()
    if (rect) {
      const maxTop = window.innerHeight - 280
      setFlyoutTop(Math.max(16, Math.min(rect.top, maxTop)))
    }
    setOpenKey(item.key)
  }

  return (
    <aside className="fixed left-0 top-0 h-full w-20 flex flex-col items-center py-4 z-50
      bg-primary-container dark:bg-[#0f1629]
      border-r border-primary/20 dark:border-[#1e2a45]
      shadow-nav">

      {/* Logo */}
      <button onClick={() => navigate('/')} className="flex flex-col items-center mb-8 hover:opacity-80 transition-opacity active:scale-95">
        <span className="text-secondary-fixed font-black text-2xl font-space">CF</span>
        <span className="text-primary-fixed-dim/50 text-[8px] font-bold tracking-wider font-space">v1.0</span>
      </button>

      {/* 대메뉴 */}
      <nav className="flex flex-col items-center flex-1 space-y-5 w-full overflow-y-auto">
        {items.map(item => {
          const active = containsPath(item, pathname)
          const open = openKey === item.key
          return (
            <button
              key={item.key}
              ref={el => { btnRefs.current[item.key] = el }}
              onClick={() => handleClick(item)}
              className={
                active
                  ? 'flex flex-col items-center justify-center w-14 h-14 bg-secondary-fixed text-on-secondary-fixed rounded-xl shadow-spark transition-transform active:scale-95 shrink-0'
                  : `group flex flex-col items-center justify-center w-full py-1.5 transition-all duration-200 shrink-0
                     ${open ? 'text-white' : 'text-primary-fixed-dim/70 hover:text-white'}`
              }
            >
              <span className="material-symbols-outlined text-[22px] group-hover:scale-110 transition-transform">
                {item.icon}
              </span>
              <span className="font-space text-[8px] uppercase tracking-wider font-medium mt-1">
                {t(`nav.${item.key}`)}
              </span>
              {item.children && (
                <span className="material-symbols-outlined text-[10px] opacity-60 -mt-0.5">
                  {open ? 'expand_less' : 'expand_more'}
                </span>
              )}
            </button>
          )
        })}
      </nav>

      {/* Support */}
      <div className="mt-auto">
        <button className="group flex flex-col items-center text-primary-fixed-dim/70 hover:text-white transition-all duration-200 py-2 w-full">
          <span className="material-symbols-outlined text-[22px] group-hover:scale-110 transition-transform">help_outline</span>
          <span className="font-space text-[8px] uppercase tracking-wider font-medium mt-1">{t('ui.support')}</span>
        </button>
      </div>

      {/* 중메뉴/소메뉴 플라이아웃 */}
      {openItem && (
        <div ref={panelRef} style={{ top: flyoutTop }}
          className="fixed left-20 w-64 max-h-[70vh] overflow-y-auto card p-2 z-50 shadow-card-md">
          <p className="px-3 py-2 text-label-md uppercase text-text-muted font-bold">{t(`nav.${openItem.key}`)}</p>
          <FlyoutList items={openItem.children} pathname={pathname} navigate={navigate} t={t} />
        </div>
      )}
    </aside>
  )
}

// 중메뉴(및 있다면 소메뉴)를 재귀 렌더링하는 플라이아웃 목록.
function FlyoutList({ items, pathname, navigate, t, depth = 0 }) {
  const [openSub, setOpenSub] = useState(() => {
    const activeParent = items.find(i => i.children && containsPath(i, pathname))
    return activeParent?.key ?? null
  })

  return (
    <div className="space-y-0.5">
      {items.map(item => {
        if (item.children) {
          const open = openSub === item.key
          const active = containsPath(item, pathname)
          return (
            <div key={item.key}>
              <button onClick={() => setOpenSub(open ? null : item.key)}
                style={{ paddingLeft: 12 + depth * 16 }}
                className={`w-full flex items-center gap-2 pr-3 py-2 rounded-lg text-sm font-semibold transition-colors
                  ${active ? 'bg-primary-container text-on-primary-container' : 'text-on-surface hover:bg-surface-container dark:text-[#e6e6f5] dark:hover:bg-[#25274a]'}`}>
                <span className="material-symbols-outlined text-[18px]">{item.icon}</span>
                <span className="flex-1 text-left">{t(`nav.${item.key}`)}</span>
                <span className="material-symbols-outlined text-[16px] opacity-60">{open ? 'expand_less' : 'expand_more'}</span>
              </button>
              {open && <FlyoutList items={item.children} pathname={pathname} navigate={navigate} t={t} depth={depth + 1} />}
            </div>
          )
        }
        const active = pathname === item.path || pathname.startsWith(item.path + '/')
        return (
          <button key={item.key} onClick={() => navigate(item.path)}
            style={{ paddingLeft: 12 + depth * 16 }}
            className={`w-full flex items-center gap-2 pr-3 py-2 rounded-lg text-sm font-medium transition-colors
              ${active ? 'bg-accent-container text-on-accent-container font-bold' : 'text-on-surface-variant hover:bg-surface-container dark:hover:bg-[#25274a]'}`}>
            <span className="material-symbols-outlined text-[18px]">{item.icon}</span>
            {t(`nav.${item.key}`)}
          </button>
        )
      })}
    </div>
  )
}
