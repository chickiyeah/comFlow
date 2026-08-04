import { useEffect } from 'react'
import SideNav from './SideNav'
import TopNav from './TopNav'
import BottomNav from './BottomNav'
import SuggestionFab from '../common/SuggestionFab'
import AssistantPanel from '../common/AssistantPanel'
import ExternalLinkModal from '../common/ExternalLinkModal'
import useExternalLink from '../../store/externalLinkStore'

export default function Layout({ title, children }) {
  // 외부 사이트로 나가는 모든 링크(<a> http/https 타 도메인) 클릭을 가로채 확인 모달 표시
  useEffect(() => {
    const handler = (e) => {
      const a = e.target.closest?.('a')
      if (!a) return
      const href = a.getAttribute('href')
      if (!href || !/^https?:\/\//i.test(href)) return
      let u
      try { u = new URL(href, window.location.href) } catch { return }
      if (u.origin === window.location.origin) return  // 내부 링크는 통과
      e.preventDefault()
      e.stopPropagation()
      useExternalLink.getState().open(href)
    }
    document.addEventListener('click', handler, true)
    return () => document.removeEventListener('click', handler, true)
  }, [])

  return (
    <div className="min-h-screen bg-background dark:bg-[#12151c] text-on-background dark:text-inverse-on-surface transition-colors duration-300">
      <div className="hidden lg:block">
        <SideNav />
      </div>
      <TopNav title={title} />
      <main className="lg:ml-20 pt-20 px-4 pb-20 lg:px-container_padding lg:pb-6 min-h-screen">
        {children}
      </main>
      <BottomNav />
      <SuggestionFab />
      <AssistantPanel />
      <ExternalLinkModal />
    </div>
  )
}
