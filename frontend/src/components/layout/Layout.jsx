import SideNav from './SideNav'
import TopNav from './TopNav'
import BottomNav from './BottomNav'
import SuggestionFab from '../common/SuggestionFab'
import AssistantPanel from '../common/AssistantPanel'

export default function Layout({ title, children }) {
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
    </div>
  )
}
