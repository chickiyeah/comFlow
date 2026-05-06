import SideNav from './SideNav'
import TopNav from './TopNav'
import BottomNav from './BottomNav'

export default function Layout({ title, children, fab }) {
  return (
    <div className="min-h-screen bg-background dark:bg-[#0b0e14] text-on-background dark:text-on-surface transition-colors duration-300">
      {/* Desktop sidebar */}
      <div className="hidden lg:block">
        <SideNav />
      </div>

      {/* TopNav */}
      <TopNav title={title} />

      {/* Main */}
      <main className="lg:ml-20 pt-16 p-4 lg:p-container_padding min-h-screen pb-20 lg:pb-6">
        {children}
      </main>

      {/* Mobile bottom nav */}
      <BottomNav />

      {/* FAB slot */}
      {fab}
    </div>
  )
}
