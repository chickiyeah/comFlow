import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getAllNotices } from '../api/notice'

export default function Notices() {
  const { t } = useTranslation()
  const [notices, setNotices] = useState([])
  const [loading, setLoading] = useState(true)
  const [openId, setOpenId] = useState(null)

  useEffect(() => {
    getAllNotices()
      .then(r => {
        const list = r.data ?? []
        setNotices(list)
        if (list.length) setOpenId(list[0].id)   // 최신 공지 자동 펼침
      })
      .catch(() => setNotices([]))
      .finally(() => setLoading(false))
  }, [])

  return (
    <Layout title={t('notices.title')}>
      <div className="max-w-3xl mx-auto">
        <div className="mb-6">
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary-fixed">campaign</span>{t('notices.title')}
          </h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('notices.subtitle')}</p>
        </div>

        {loading ? (
          <div className="flex justify-center py-20"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
        ) : notices.length === 0 ? (
          <div className="card p-12 text-center">
            <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600 mb-3">notifications_off</span>
            <p className="text-on-surface-variant dark:text-slate-400">{t('notices.empty')}</p>
          </div>
        ) : (
          <div className="space-y-3">
            {notices.map(n => {
              const open = openId === n.id
              return (
                <div key={n.id} className="card overflow-hidden">
                  <button onClick={() => setOpenId(open ? null : n.id)}
                    className="w-full text-left p-5 flex items-start gap-3 hover:bg-surface-container-low dark:hover:bg-slate-800/50 transition-colors">
                    {n.important && <span className="text-[10px] font-black px-2 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded shrink-0 mt-0.5">{t('notices.important')}</span>}
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-primary dark:text-white">{n.title}</p>
                      {n.summary && !open && <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1 truncate">{n.summary}</p>}
                      <p className="text-xs text-outline dark:text-slate-500 mt-1.5">{new Date(n.createdAt).toLocaleString('ko')}</p>
                    </div>
                    <span className={`material-symbols-outlined text-outline dark:text-slate-400 shrink-0 transition-transform ${open ? 'rotate-180' : ''}`}>expand_more</span>
                  </button>
                  {open && (
                    <div className="px-5 pb-5 -mt-1">
                      <div className="border-t border-slate-100 dark:border-slate-800 pt-4">
                        <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap leading-relaxed">{n.content || n.summary || t('notices.noContent')}</p>
                      </div>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </div>
    </Layout>
  )
}
