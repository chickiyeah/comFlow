import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useThemeStore = create(
  persist(
    (set, get) => ({
      dark: false,
      toggle: () => {
        const next = !get().dark
        document.documentElement.classList.toggle('dark', next)
        set({ dark: next })
      },
      init: () => {
        const stored = JSON.parse(localStorage.getItem('campusflow-theme') || '{}')
        const isDark = stored?.state?.dark ?? false
        document.documentElement.classList.toggle('dark', isDark)
      },
    }),
    { name: 'campusflow-theme' }
  )
)

export default useThemeStore
