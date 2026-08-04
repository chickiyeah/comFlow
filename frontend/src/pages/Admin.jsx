import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getAllNotices, createNotice, deleteNotice } from '../api/notice'
import { getAllSuggestions, replySuggestion } from '../api/suggestion'
import { getDeptInfos, createDeptInfo, updateDeptInfo, deleteDeptInfo } from '../api/deptinfo'

const EMPTY_DEPT = { category: 'ADMISSION', title: '', content: '', keywords: '', active: true }
const DEPT_CATS = ['ADMISSION', 'FACULTY', 'SCHOLARSHIP', 'CURRICULUM', 'GENERAL']

const STATUS_STYLE = {
  PENDING:   'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300',
  IN_REVIEW: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
  RESOLVED:  'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  REJECTED:  'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
}

export default function Admin() {
  const { t } = useTranslation()

  const CAT_LABEL = {
    ACADEMIC: t('admin.cat_ACADEMIC'), FACILITY: t('admin.cat_FACILITY'), WELFARE: t('admin.cat_WELFARE'), CURRICULUM: t('admin.cat_CURRICULUM'), GENERAL: t('admin.cat_GENERAL'),
  }
  const STATUS_LABEL = {
    PENDING: t('admin.status_PENDING'), IN_REVIEW: t('admin.status_IN_REVIEW'), RESOLVED: t('admin.status_RESOLVED'), REJECTED: t('admin.status_REJECTED'),
  }
  const DEPT_CAT_LABEL = {
    ADMISSION: t('admin.deptCat_ADMISSION'), FACULTY: t('admin.deptCat_FACULTY'), SCHOLARSHIP: t('admin.deptCat_SCHOLARSHIP'), CURRICULUM: t('admin.deptCat_CURRICULUM'), GENERAL: t('admin.deptCat_GENERAL'),
  }

  const [tab, setTab] = useState('notices')

  // 내부정보
  const [deptInfos, setDeptInfos] = useState([])
  const [showDeptForm, setShowDeptForm] = useState(false)
  const [editingDept, setEditingDept] = useState(null)
  const [deptForm, setDeptForm] = useState(EMPTY_DEPT)

  // 공지
  const [notices, setNotices] = useState([])
  const [showNoticeForm, setShowNoticeForm] = useState(false)
  const [noticeForm, setNoticeForm] = useState({ title: '', summary: '', content: '', important: false })

  // 건의함
  const [suggestions, setSuggestions] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [replyTarget, setReplyTarget] = useState(null)
  const [replyForm, setReplyForm] = useState({ reply: '', status: 'RESOLVED' })

  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (tab === 'notices') loadNotices()
    if (tab === 'suggestions') loadSuggestions(0)
    if (tab === 'deptinfo') loadDeptInfos()
  }, [tab])

  const loadDeptInfos = async () => {
    setLoading(true)
    try { const r = await getDeptInfos(); setDeptInfos(r.data ?? []) }
    catch { setDeptInfos([]) }
    finally { setLoading(false) }
  }
  const openCreateDept = () => { setEditingDept(null); setDeptForm(EMPTY_DEPT); setShowDeptForm(true) }
  const openEditDept = (d) => {
    setEditingDept(d)
    setDeptForm({ category: d.category, title: d.title, content: d.content, keywords: d.keywords ?? '', active: d.active })
    setShowDeptForm(true)
  }
  const handleSaveDept = async (e) => {
    e.preventDefault()
    try {
      if (editingDept) await updateDeptInfo(editingDept.id, deptForm)
      else await createDeptInfo(deptForm)
      setShowDeptForm(false); loadDeptInfos()
    } catch { alert(t('admin.alertCreateFail')) }
  }
  const handleDeleteDept = async (id) => {
    if (!confirm(t('admin.confirmDeleteDept'))) return
    try { await deleteDeptInfo(id); setDeptInfos(prev => prev.filter(d => d.id !== id)) }
    catch { alert(t('admin.alertDeleteFail')) }
  }

  const loadNotices = async () => {
    setLoading(true)
    try {
      const r = await getAllNotices()
      setNotices(r.data ?? [])
    } catch { setNotices([]) }
    finally { setLoading(false) }
  }

  const loadSuggestions = async (p) => {
    setLoading(true)
    try {
      const r = await getAllSuggestions(p, 20)
      const pageData = r.data
      setSuggestions(pageData?.content ?? [])
      setTotalPages(pageData?.totalPages ?? 0)
      setPage(p)
    } catch { setSuggestions([]); setTotalPages(0) }
    finally { setLoading(false) }
  }

  const handleCreateNotice = async (e) => {
    e.preventDefault()
    try {
      await createNotice(noticeForm)
      setNoticeForm({ title: '', summary: '', content: '', important: false })
      setShowNoticeForm(false)
      loadNotices()
    } catch {
      alert(t('admin.alertCreateFail'))
    }
  }

  const handleDeleteNotice = async (id) => {
    if (!confirm(t('admin.confirmDeleteNotice'))) return
    try {
      await deleteNotice(id)
      setNotices(prev => prev.filter(n => n.id !== id))
    } catch { alert(t('admin.alertDeleteFail')) }
  }

  const handleReply = async (e) => {
    e.preventDefault()
    if (!replyTarget) return
    try {
      await replySuggestion(replyTarget.id, replyForm.reply, replyForm.status)
      setReplyTarget(null)
      setReplyForm({ reply: '', status: 'RESOLVED' })
      loadSuggestions(page)
    } catch { alert(t('admin.alertReplyFail')) }
  }

  return (
    <Layout>
      <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white mb-6 flex items-center gap-2">
        <span className="material-symbols-outlined text-secondary-fixed">admin_panel_settings</span>
        {t('admin.title')}
      </h1>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {[
          { key: 'notices',     label: t('admin.tab_notices'),     icon: 'campaign' },
          { key: 'suggestions', label: t('admin.tab_suggestions'), icon: 'forum' },
          { key: 'deptinfo',    label: t('admin.tab_deptInfo'),    icon: 'school' },
        ].map(tabItem => (
          <button key={tabItem.key} onClick={() => setTab(tabItem.key)}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
              tab === tabItem.key
                ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm'
                : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}>
            <span className="material-symbols-outlined text-[18px]">{tabItem.icon}</span>{tabItem.label}
          </button>
        ))}
      </div>

      {/* ── 공지사항 탭 ── */}
      {tab === 'notices' && (
        <>
          <div className="flex justify-between items-center mb-4">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('admin.totalCount', { count: notices.length })}</p>
            <button onClick={() => setShowNoticeForm(true)} className="btn-primary text-sm flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('admin.writeNotice')}
            </button>
          </div>

          {loading ? (
            <div className="card p-12 text-center text-outline dark:text-slate-500">{t('admin.loading')}</div>
          ) : notices.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600">campaign</span>
              <p className="mt-3 text-on-surface-variant dark:text-slate-400">{t('admin.noNotices')}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {notices.map(n => (
                <div key={n.id} className="card p-5 flex items-start gap-3">
                  {n.important && <span className="text-xs font-bold px-2 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded shrink-0">{t('admin.important')}</span>}
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-primary dark:text-white">{n.title}</p>
                    {n.summary && <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{n.summary}</p>}
                    <p className="text-xs text-outline dark:text-slate-500 mt-2">{new Date(n.createdAt).toLocaleString('ko')}</p>
                  </div>
                  <button onClick={() => handleDeleteNotice(n.id)}
                    className="p-2 rounded-lg bg-error-container dark:bg-error/20 text-error hover:scale-110 transition-transform">
                    <span className="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── 건의함 탭 ── */}
      {tab === 'suggestions' && (
        <>
          <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-4">{t('admin.suggestionsDesc')}</p>

          {loading ? (
            <div className="card p-12 text-center text-outline dark:text-slate-500">{t('admin.loading')}</div>
          ) : suggestions.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600">forum</span>
              <p className="mt-3 text-on-surface-variant dark:text-slate-400">{t('admin.noSuggestions')}</p>
            </div>
          ) : (
            <>
              <div className="space-y-3">
                {suggestions.map(s => (
                  <div key={s.id} className="card p-5">
                    <div className="flex items-center gap-2 mb-2 flex-wrap">
                      <span className="text-[10px] font-bold px-2 py-0.5 bg-primary/10 dark:bg-primary-container/30 text-primary dark:text-secondary-fixed rounded">
                        {CAT_LABEL[s.category] || s.category}
                      </span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${STATUS_STYLE[s.status]}`}>
                        {STATUS_LABEL[s.status] || s.status}
                      </span>
                      <span className="text-xs text-outline dark:text-slate-500 ml-auto">{new Date(s.createdAt).toLocaleString('ko')}</span>
                    </div>
                    <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{s.content}</p>
                    {s.adminReply && (
                      <div className="mt-3 p-3 bg-secondary-container/20 dark:bg-secondary-fixed/10 border-l-4 border-secondary-fixed rounded">
                        <p className="text-xs font-bold text-primary dark:text-secondary-fixed mb-1">{t('admin.adminReply')}</p>
                        <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{s.adminReply}</p>
                      </div>
                    )}
                    {s.status === 'PENDING' || s.status === 'IN_REVIEW' ? (
                      <button onClick={() => { setReplyTarget(s); setReplyForm({ reply: s.adminReply ?? '', status: 'RESOLVED' }) }}
                        className="mt-3 text-xs font-bold px-3 py-1.5 rounded-lg bg-primary/10 dark:bg-primary-container/20 text-primary dark:text-secondary-fixed hover:bg-primary/20 transition-colors">
                        {t('admin.replyBtn')}
                      </button>
                    ) : null}
                  </div>
                ))}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-6">
                  <button disabled={page === 0} onClick={() => loadSuggestions(page - 1)}
                    className="px-3 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-sm disabled:opacity-40">{t('admin.prev')}</button>
                  <span className="px-3 py-1.5 text-sm text-on-surface-variant dark:text-slate-400">{page + 1} / {totalPages}</span>
                  <button disabled={page + 1 >= totalPages} onClick={() => loadSuggestions(page + 1)}
                    className="px-3 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-sm disabled:opacity-40">{t('admin.next')}</button>
                </div>
              )}
            </>
          )}
        </>
      )}

      {/* ── 내부정보 탭 ── */}
      {tab === 'deptinfo' && (
        <>
          <div className="flex justify-between items-center mb-4">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('admin.deptDesc')}</p>
            <button onClick={openCreateDept} className="btn-primary text-sm flex items-center gap-1.5 shrink-0">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('admin.deptAdd')}
            </button>
          </div>

          {loading ? (
            <div className="card p-12 text-center text-outline dark:text-slate-500">{t('admin.loading')}</div>
          ) : deptInfos.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600">school</span>
              <p className="mt-3 text-on-surface-variant dark:text-slate-400">{t('admin.deptEmpty')}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {deptInfos.map(d => (
                <div key={d.id} className="card p-5 flex items-start gap-3">
                  <span className="text-[10px] font-bold px-2 py-0.5 bg-primary/10 dark:bg-primary-container/30 text-primary dark:text-secondary-fixed rounded shrink-0">
                    {DEPT_CAT_LABEL[d.category] || d.category}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-bold text-primary dark:text-white">{d.title}</p>
                      {!d.active && <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-outline dark:text-slate-500">{t('admin.deptInactive')}</span>}
                    </div>
                    <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1 line-clamp-2 whitespace-pre-wrap">{d.content}</p>
                  </div>
                  <div className="flex gap-1.5 shrink-0">
                    <button onClick={() => openEditDept(d)}
                      className="p-2 rounded-lg bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:scale-110 transition-transform">
                      <span className="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button onClick={() => handleDeleteDept(d.id)}
                      className="p-2 rounded-lg bg-error-container dark:bg-error/20 text-error hover:scale-110 transition-transform">
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── 내부정보 작성/수정 모달 ── */}
      {showDeptForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setShowDeptForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{editingDept ? t('admin.deptEdit') : t('admin.deptAdd')}</h3>
              <button onClick={() => setShowDeptForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSaveDept} className="p-6 space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.deptCategory')}</label>
                <div className="flex gap-2 flex-wrap">
                  {DEPT_CATS.map(c => (
                    <button key={c} type="button" onClick={() => setDeptForm(f => ({...f, category: c}))}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
                        deptForm.category === c ? 'bg-primary dark:bg-primary-container text-white' : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300'
                      }`}>
                      {DEPT_CAT_LABEL[c]}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.deptTitle')}</label>
                <input required value={deptForm.title} onChange={e => setDeptForm(f => ({...f, title: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.deptContent')}</label>
                <textarea required value={deptForm.content} onChange={e => setDeptForm(f => ({...f, content: e.target.value}))}
                  rows={8}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.deptKeywords')}</label>
                <input value={deptForm.keywords} onChange={e => setDeptForm(f => ({...f, keywords: e.target.value}))}
                  placeholder={t('admin.deptKeywordsPlaceholder')}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={deptForm.active} onChange={e => setDeptForm(f => ({...f, active: e.target.checked}))}
                  className="w-4 h-4 accent-secondary-fixed" />
                <span className="text-sm text-on-surface dark:text-slate-200">{t('admin.deptActiveLabel')}</span>
              </label>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowDeptForm(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">
                  {t('common.cancel')}
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {t('common.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── 공지 작성 모달 ── */}
      {showNoticeForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setShowNoticeForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('admin.writeNotice')}</h3>
              <button onClick={() => setShowNoticeForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreateNotice} className="p-6 space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.fieldTitle')}</label>
                <input required value={noticeForm.title} onChange={e => setNoticeForm(f => ({...f, title: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.fieldSummary')}</label>
                <input value={noticeForm.summary} onChange={e => setNoticeForm(f => ({...f, summary: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.fieldContent')}</label>
                <textarea value={noticeForm.content} onChange={e => setNoticeForm(f => ({...f, content: e.target.value}))}
                  rows={8}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={noticeForm.important} onChange={e => setNoticeForm(f => ({...f, important: e.target.checked}))}
                  className="w-4 h-4 accent-secondary-fixed" />
                <span className="text-sm text-on-surface dark:text-slate-200">{t('admin.markImportant')}</span>
              </label>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowNoticeForm(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">
                  {t('common.cancel')}
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {t('admin.submitNotice')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── 답변 모달 ── */}
      {replyTarget && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setReplyTarget(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('admin.replyTitle')}</h3>
              <button onClick={() => setReplyTarget(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleReply} className="p-6 space-y-4">
              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('admin.originalSuggestion')}</p>
                <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{replyTarget.content}</p>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.fieldReply')}</label>
                <textarea required value={replyForm.reply} onChange={e => setReplyForm(f => ({...f, reply: e.target.value}))}
                  rows={6}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('admin.fieldStatus')}</label>
                <div className="flex gap-2 flex-wrap">
                  {['IN_REVIEW', 'RESOLVED', 'REJECTED'].map(s => (
                    <button key={s} type="button" onClick={() => setReplyForm(f => ({...f, status: s}))}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
                        replyForm.status === s
                          ? 'bg-primary dark:bg-primary-container text-white'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300'
                      }`}>
                      {STATUS_LABEL[s]}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setReplyTarget(null)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">
                  {t('common.cancel')}
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {t('admin.submitReply')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
