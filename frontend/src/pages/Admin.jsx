import { useState, useEffect } from 'react'
import Layout from '../components/layout/Layout'
import { getAllNotices, createNotice, deleteNotice } from '../api/notice'
import { getAllSuggestions, replySuggestion } from '../api/suggestion'

const CAT_LABEL = {
  ACADEMIC: '학사', FACILITY: '시설', WELFARE: '복지', CURRICULUM: '교육과정', GENERAL: '일반',
}
const STATUS_LABEL = {
  PENDING: '접수', IN_REVIEW: '검토중', RESOLVED: '처리완료', REJECTED: '반려',
}
const STATUS_STYLE = {
  PENDING:   'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300',
  IN_REVIEW: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
  RESOLVED:  'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  REJECTED:  'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
}

export default function Admin() {
  const [tab, setTab] = useState('notices')

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
  }, [tab])

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
      alert('공지 작성 실패. ADMIN 권한이 필요합니다.')
    }
  }

  const handleDeleteNotice = async (id) => {
    if (!confirm('공지를 삭제하시겠습니까?')) return
    try {
      await deleteNotice(id)
      setNotices(prev => prev.filter(n => n.id !== id))
    } catch { alert('삭제 실패') }
  }

  const handleReply = async (e) => {
    e.preventDefault()
    if (!replyTarget) return
    try {
      await replySuggestion(replyTarget.id, replyForm.reply, replyForm.status)
      setReplyTarget(null)
      setReplyForm({ reply: '', status: 'RESOLVED' })
      loadSuggestions(page)
    } catch { alert('답변 등록 실패') }
  }

  return (
    <Layout>
      <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white mb-6 flex items-center gap-2">
        <span className="material-symbols-outlined text-secondary-fixed">admin_panel_settings</span>
        관리자 페이지
      </h1>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {[
          { key: 'notices',     label: '공지사항', icon: 'campaign' },
          { key: 'suggestions', label: '건의함',   icon: 'forum' },
        ].map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
              tab === t.key
                ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm'
                : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}>
            <span className="material-symbols-outlined text-[18px]">{t.icon}</span>{t.label}
          </button>
        ))}
      </div>

      {/* ── 공지사항 탭 ── */}
      {tab === 'notices' && (
        <>
          <div className="flex justify-between items-center mb-4">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">총 {notices.length}건</p>
            <button onClick={() => setShowNoticeForm(true)} className="btn-primary text-sm flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px]">add</span>공지 작성
            </button>
          </div>

          {loading ? (
            <div className="card p-12 text-center text-outline dark:text-slate-500">불러오는 중…</div>
          ) : notices.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600">campaign</span>
              <p className="mt-3 text-on-surface-variant dark:text-slate-400">등록된 공지가 없습니다.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {notices.map(n => (
                <div key={n.id} className="card p-5 flex items-start gap-3">
                  {n.important && <span className="text-xs font-bold px-2 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded shrink-0">중요</span>}
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
          <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-4">학생 익명 건의 — 답변 후 학생 알림 (이메일 미발송)</p>

          {loading ? (
            <div className="card p-12 text-center text-outline dark:text-slate-500">불러오는 중…</div>
          ) : suggestions.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600">forum</span>
              <p className="mt-3 text-on-surface-variant dark:text-slate-400">건의가 없습니다.</p>
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
                        <p className="text-xs font-bold text-primary dark:text-secondary-fixed mb-1">관리자 답변</p>
                        <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{s.adminReply}</p>
                      </div>
                    )}
                    {s.status === 'PENDING' || s.status === 'IN_REVIEW' ? (
                      <button onClick={() => { setReplyTarget(s); setReplyForm({ reply: s.adminReply ?? '', status: 'RESOLVED' }) }}
                        className="mt-3 text-xs font-bold px-3 py-1.5 rounded-lg bg-primary/10 dark:bg-primary-container/20 text-primary dark:text-secondary-fixed hover:bg-primary/20 transition-colors">
                        답변하기
                      </button>
                    ) : null}
                  </div>
                ))}
              </div>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-6">
                  <button disabled={page === 0} onClick={() => loadSuggestions(page - 1)}
                    className="px-3 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-sm disabled:opacity-40">이전</button>
                  <span className="px-3 py-1.5 text-sm text-on-surface-variant dark:text-slate-400">{page + 1} / {totalPages}</span>
                  <button disabled={page + 1 >= totalPages} onClick={() => loadSuggestions(page + 1)}
                    className="px-3 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-sm disabled:opacity-40">다음</button>
                </div>
              )}
            </>
          )}
        </>
      )}

      {/* ── 공지 작성 모달 ── */}
      {showNoticeForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setShowNoticeForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">공지 작성</h3>
              <button onClick={() => setShowNoticeForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreateNotice} className="p-6 space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">제목 *</label>
                <input required value={noticeForm.title} onChange={e => setNoticeForm(f => ({...f, title: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">요약 (한 줄)</label>
                <input value={noticeForm.summary} onChange={e => setNoticeForm(f => ({...f, summary: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">본문</label>
                <textarea value={noticeForm.content} onChange={e => setNoticeForm(f => ({...f, content: e.target.value}))}
                  rows={8}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={noticeForm.important} onChange={e => setNoticeForm(f => ({...f, important: e.target.checked}))}
                  className="w-4 h-4 accent-secondary-fixed" />
                <span className="text-sm text-on-surface dark:text-slate-200">중요 공지로 표시</span>
              </label>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowNoticeForm(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">
                  취소
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  공지 등록
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
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">건의 답변</h3>
              <button onClick={() => setReplyTarget(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleReply} className="p-6 space-y-4">
              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                <p className="text-xs text-outline dark:text-slate-500 mb-1">원본 건의</p>
                <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{replyTarget.content}</p>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">답변 *</label>
                <textarea required value={replyForm.reply} onChange={e => setReplyForm(f => ({...f, reply: e.target.value}))}
                  rows={6}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">처리 상태</label>
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
                  취소
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  답변 등록
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
