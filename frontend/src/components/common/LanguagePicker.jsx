import { useState } from 'react'
import { useTranslation } from 'react-i18next'

const LANGUAGES = [
  { code: 'ko', label: '한국어',     flag: '🇰🇷' },
  { code: 'en', label: 'English',    flag: '🇺🇸' },
  { code: 'zh', label: '中文',       flag: '🇨🇳' },
  { code: 'ja', label: '日本語',     flag: '🇯🇵' },
  { code: 'vi', label: 'Tiếng Việt', flag: '🇻🇳' },
]

export { LANGUAGES }

export default function LanguagePicker() {
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(false)
  const current = LANGUAGES.find(l => l.code === i18n.language) ?? LANGUAGES[0]

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(v => !v)}
        className="flex items-center gap-1.5 px-3 py-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:scale-105 transition-transform text-sm font-medium"
      >
        <span>{current.flag}</span>
        <span>{current.code.toUpperCase()}</span>
        <span className="material-symbols-outlined text-[14px]">expand_more</span>
      </button>

      {open && (
        <div className="absolute right-0 top-11 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-100 dark:border-slate-800 py-1.5 w-44 z-50 overflow-hidden">
          {LANGUAGES.map(lang => (
            <button
              key={lang.code}
              onClick={() => { i18n.changeLanguage(lang.code); setOpen(false) }}
              className={`w-full text-left px-4 py-2.5 text-sm flex items-center gap-3 hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors ${
                i18n.language === lang.code
                  ? 'text-primary dark:text-secondary-fixed font-bold'
                  : 'text-on-surface dark:text-slate-300'
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
      )}
    </div>
  )
}
