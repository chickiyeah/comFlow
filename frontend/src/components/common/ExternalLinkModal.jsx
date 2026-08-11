import { useTranslation } from 'react-i18next'
import useExternalLink from '../../store/externalLinkStore'

export default function ExternalLinkModal() {
  const { t } = useTranslation()
  const url = useExternalLink(s => s.url)
  const close = useExternalLink(s => s.close)

  if (!url) return null

  const confirm = () => {
    window.open(url, '_blank', 'noopener,noreferrer')
    close()
  }

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center p-4" onClick={close}>
      <div className="absolute inset-0 bg-black/50 dark:bg-black/60" />
      <div onClick={e => e.stopPropagation()}
        className="relative bg-white dark:bg-slate-900 rounded-2xl p-6 w-full max-w-sm shadow-xl">
        <div className="flex items-center gap-2 mb-3">
          <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">open_in_new</span>
          <h3 className="font-bold text-primary dark:text-white">{t('ui.externalLink.title')}</h3>
        </div>
        <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-2">{t('ui.externalLink.desc')}</p>
        <p className="text-xs text-primary dark:text-secondary-fixed break-all bg-surface-container-low dark:bg-slate-800 rounded-lg px-3 py-2 mb-5">{url}</p>
        <div className="flex gap-2">
          <button onClick={close}
            className="flex-1 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm font-medium text-outline dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800">
            {t('ui.externalLink.cancel')}
          </button>
          <button onClick={confirm}
            className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:opacity-90">
            {t('ui.externalLink.confirm')}
          </button>
        </div>
      </div>
    </div>
  )
}
