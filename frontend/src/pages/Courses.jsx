import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import useAuthStore from '../store/authStore'
import { downloadCertificate } from '../api/course'
import { getCourses, createCourse, deleteCourse, recordCourseView, completeCourse,
  getCourseComments, addCourseComment, deleteCourseComment,
  saveCourseProgress, getMyCourseProgress } from '../api/course'

const EMPTY = { title: '', description: '', videoUrl: '', thumbnailUrl: '', category: '', active: true }

// YouTube videoId 추출
function ytId(url) {
  if (!url) return null
  try {
    const u = new URL(url)
    if (u.hostname.includes('youtu.be')) return u.pathname.slice(1)
    if (u.searchParams.get('v')) return u.searchParams.get('v')
    if (u.pathname.startsWith('/embed/')) return u.pathname.split('/embed/')[1]
  } catch { /* ignore */ }
  return null
}
// YouTube IFrame API 로더 (1회)
let ytApiPromise = null
function loadYTApi() {
  if (window.YT && window.YT.Player) return Promise.resolve(window.YT)
  if (ytApiPromise) return ytApiPromise
  ytApiPromise = new Promise(resolve => {
    const tag = document.createElement('script')
    tag.src = 'https://www.youtube.com/iframe_api'
    document.head.appendChild(tag)
    const prev = window.onYouTubeIframeAPIReady
    window.onYouTubeIframeAPIReady = () => { if (prev) { try { prev() } catch { /**/ } } resolve(window.YT) }
  })
  return ytApiPromise
}

// 다양한 영상 URL → 임베드 URL
function toEmbed(url) {
  if (!url) return null
  try {
    const u = new URL(url)
    if (u.hostname.includes('youtube.com') && u.searchParams.get('v')) return `https://www.youtube.com/embed/${u.searchParams.get('v')}`
    if (u.hostname.includes('youtu.be')) return `https://www.youtube.com/embed/${u.pathname.slice(1)}`
    if (u.hostname.includes('youtube.com') && u.pathname.startsWith('/embed/')) return url
    if (u.hostname.includes('vimeo.com')) return `https://player.vimeo.com/video/${u.pathname.split('/').filter(Boolean).pop()}`
    return url
  } catch { return url }
}
function isFile(url) { return /\.(mp4|webm|ogg)(\?|$)/i.test(url || '') }

export default function Courses() {
  const { t } = useTranslation()
  const user = useAuthStore(s => s.user)
  const isStaff = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_PROFESSOR'

  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [watch, setWatch] = useState(null)         // 시청 중 강좌
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  // Q&A
  const [comments, setComments] = useState([])
  const [cLoading, setCLoading] = useState(false)
  const [cText, setCText] = useState('')
  const [cQuestion, setCQuestion] = useState(true)
  const [cSending, setCSending] = useState(false)
  const [completed, setCompleted] = useState(false)
  const [completing, setCompleting] = useState(false)
  // 진도율
  const [progressMap, setProgressMap] = useState({})
  const videoRef = useRef(null)
  const ytRef = useRef(null)
  const ytTimerRef = useRef(null)
  const resumeSecRef = useRef(0)
  const lastSaveRef = useRef(0)

  const saveProg = (course, pos, dur) => {
    const p = Math.floor(pos || 0), d = Math.floor(dur || 0)
    saveCourseProgress(course.id, { positionSec: p, durationSec: d }).catch(() => {})
    setProgressMap(prev => ({
      ...prev,
      [course.id]: {
        courseId: course.id, lastPositionSec: p, durationSec: d,
        completed: (prev[course.id]?.completed) || (d > 0 && p >= d * 0.9),
      },
    }))
    if (d > 0 && p >= d * 0.9) setCompleted(true)
  }

  // YouTube 플레이어 — 시청 모달 열릴 때 생성, 닫힐 때 최종 저장+파기
  useEffect(() => {
    if (!watch) return
    const vid = !isFile(watch.videoUrl) ? ytId(watch.videoUrl) : null
    if (!vid) return
    const course = watch
    let cancelled = false
    loadYTApi().then(YT => {
      if (cancelled) return
      ytRef.current = new YT.Player('yt-player-mount', {
        videoId: vid,
        playerVars: { autoplay: 1, start: Math.max(0, Math.floor(resumeSecRef.current || 0)) },
        events: { onReady: (e) => { try { if (resumeSecRef.current > 3) e.target.seekTo(resumeSecRef.current, true) } catch { /**/ } } },
      })
      ytTimerRef.current = setInterval(() => {
        const p = ytRef.current
        try { if (p && p.getCurrentTime) saveProg(course, p.getCurrentTime(), p.getDuration()) } catch { /**/ }
      }, 10000)
    })
    return () => {
      cancelled = true
      clearInterval(ytTimerRef.current)
      try { const p = ytRef.current; if (p && p.getCurrentTime) saveProg(course, p.getCurrentTime(), p.getDuration()) } catch { /**/ }
      try { if (ytRef.current && ytRef.current.destroy) ytRef.current.destroy() } catch { /**/ }
      ytRef.current = null
    }
  }, [watch])

  const handleComplete = async () => {
    if (!watch || completing) return
    setCompleting(true)
    try { await completeCourse(watch.id); setCompleted(true) }
    catch { /* ignore */ }
    finally { setCompleting(false) }
  }

  const handleCertificate = async () => {
    if (!watch) return
    try {
      const res = await downloadCertificate(watch.id)
      const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const a = document.createElement('a')
      a.href = url; a.download = `certificate-${watch.id}.pdf`
      document.body.appendChild(a); a.click(); a.remove()
      URL.revokeObjectURL(url)
    } catch { /* ignore */ }
  }

  const load = () => {
    setLoading(true)
    Promise.all([
      getCourses().then(r => setCourses(r.data ?? [])).catch(() => setCourses([])),
      getMyCourseProgress().then(r => {
        const m = {}; (r.data ?? []).forEach(p => { m[p.courseId] = p }); setProgressMap(m)
      }).catch(() => {}),
    ]).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const openWatch = (c) => {
    const prog = progressMap[c.id]
    resumeSecRef.current = prog?.lastPositionSec || 0
    lastSaveRef.current = 0
    setCompleted(prog?.completed || false)
    setWatch(c)
    recordCourseView(c.id).catch(() => {})
    setCourses(prev => prev.map(x => x.id === c.id ? { ...x, viewCount: x.viewCount + 1 } : x))
    loadComments(c.id)
  }
  const progressPct = (id) => {
    const p = progressMap[id]
    if (!p || !p.durationSec) return p?.completed ? 100 : 0
    return Math.min(100, Math.round((p.lastPositionSec / p.durationSec) * 100))
  }

  const loadComments = (id) => {
    setCLoading(true); setComments([])
    getCourseComments(id).then(r => setComments(r.data ?? [])).catch(() => setComments([])).finally(() => setCLoading(false))
  }
  const submitComment = async (e) => {
    e.preventDefault()
    if (!cText.trim() || cSending || !watch) return
    setCSending(true)
    try {
      const r = await addCourseComment(watch.id, { content: cText.trim(), question: cQuestion, parentId: null })
      setComments(r.data ?? [])
      setCText('')
    } catch { /* ignore */ }
    finally { setCSending(false) }
  }
  const removeComment = async (commentId) => {
    try { await deleteCourseComment(commentId); setComments(prev => prev.filter(c => c.id !== commentId)) } catch { /* ignore */ }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!form.title.trim() || !form.videoUrl.trim() || saving) return
    setSaving(true)
    try { await createCourse(form); setShowForm(false); setForm(EMPTY); load() }
    catch { alert(t('courses.saveFailed')) }
    finally { setSaving(false) }
  }
  const handleDelete = async (id) => {
    if (!confirm(t('courses.confirmDelete'))) return
    try { await deleteCourse(id); setCourses(prev => prev.filter(c => c.id !== id)) } catch { /* ignore */ }
  }

  return (
    <Layout title={t('courses.title')}>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary-fixed">smart_display</span>{t('courses.title')}
          </h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('courses.subtitle')}</p>
        </div>
        {isStaff && (
          <button onClick={() => { setForm(EMPTY); setShowForm(true) }}
            className="px-4 py-2.5 rounded-xl text-sm font-bold bg-secondary-fixed text-primary flex items-center gap-1.5 shrink-0">
            <span className="material-symbols-outlined text-[18px]">add</span>{t('courses.register')}
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
      ) : courses.length === 0 ? (
        <div className="card p-12 text-center">
          <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600 mb-3">video_library</span>
          <p className="font-bold text-primary dark:text-white">{t('courses.emptyTitle')}</p>
          <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('courses.emptySubtitle')}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {courses.map(c => (
            <div key={c.id} className="card overflow-hidden flex flex-col group">
              <button onClick={() => openWatch(c)} className="relative aspect-video bg-surface-container dark:bg-slate-800 flex items-center justify-center overflow-hidden">
                {c.thumbnailUrl
                  ? <img src={c.thumbnailUrl} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform" />
                  : <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600">smart_display</span>}
                <span className="absolute inset-0 flex items-center justify-center bg-black/0 group-hover:bg-black/30 transition-colors">
                  <span className="material-symbols-outlined text-white text-[48px] opacity-0 group-hover:opacity-100 transition-opacity">play_circle</span>
                </span>
                {c.category && <span className="absolute top-2 left-2 text-[10px] font-bold px-2 py-0.5 rounded-full bg-primary/80 text-white">{c.category}</span>}
                {progressMap[c.id]?.completed && <span className="absolute top-2 right-2 text-[10px] font-black px-2 py-0.5 rounded-full bg-green-500 text-white flex items-center gap-0.5"><span className="material-symbols-outlined text-[12px]">check</span>{t('courses.completed')}</span>}
                {progressPct(c.id) > 0 && (
                  <span className="absolute bottom-0 left-0 right-0 h-1 bg-black/30">
                    <span className="block h-full bg-secondary-fixed" style={{ width: `${progressPct(c.id)}%` }} />
                  </span>
                )}
              </button>
              <div className="p-4 flex-1 flex flex-col">
                <p className="font-bold text-primary dark:text-white line-clamp-2">{c.title}</p>
                <p className="text-xs text-outline dark:text-slate-400 mt-1">{c.instructorName} · {t('courses.views', { count: c.viewCount })}</p>
                <div className="mt-auto pt-3 flex items-center gap-2">
                  <button onClick={() => openWatch(c)} className="flex-1 py-2 rounded-lg bg-primary dark:bg-primary-container text-white text-sm font-bold flex items-center justify-center gap-1.5">
                    <span className="material-symbols-outlined text-[18px]">play_arrow</span>{t('courses.watch')}
                  </button>
                  {isStaff && (
                    <button onClick={() => handleDelete(c.id)} className="p-2 rounded-lg bg-error-container dark:bg-error/20 text-error">
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 시청 모달 */}
      {watch && (
        <div className="fixed inset-0 bg-black/70 z-[60] flex items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setWatch(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-2xl w-full max-w-3xl shadow-2xl overflow-hidden max-h-[92vh] overflow-y-auto">
            <div className="aspect-video bg-black">
              {isFile(watch.videoUrl) ? (
                <video ref={videoRef} src={watch.videoUrl} controls autoPlay className="w-full h-full"
                  onLoadedMetadata={e => { const r = resumeSecRef.current; if (r > 3 && r < e.target.duration - 5) e.target.currentTime = r }}
                  onTimeUpdate={e => { const now = Date.now(); if (now - lastSaveRef.current > 10000) { lastSaveRef.current = now; saveProg(watch, e.target.currentTime, e.target.duration) } }}
                  onEnded={e => saveProg(watch, e.target.duration, e.target.duration)} />
              ) : ytId(watch.videoUrl) ? (
                <div id="yt-player-mount" className="w-full h-full" />
              ) : (
                <iframe src={toEmbed(watch.videoUrl)} title={watch.title} className="w-full h-full" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowFullScreen />
              )}
            </div>
            <div className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="font-bold text-lg text-primary dark:text-white">{watch.title}</h3>
                  <p className="text-xs text-outline dark:text-slate-400 mt-0.5">{watch.instructorName} · {t('courses.views', { count: watch.viewCount })}</p>
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  <button onClick={handleComplete} disabled={completing || completed}
                    className={`px-3 py-2 rounded-xl text-sm font-bold flex items-center gap-1.5 disabled:opacity-60 ${completed ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 'bg-secondary-fixed text-primary'}`}>
                    <span className="material-symbols-outlined text-[18px]">{completed ? 'check_circle' : 'task_alt'}</span>
                    {completed ? t('courses.completed') : (completing ? t('courses.completing') : t('courses.complete'))}
                  </button>
                  {completed && (
                    <button onClick={handleCertificate}
                      className="px-3 py-2 rounded-xl text-sm font-bold flex items-center gap-1.5 bg-primary dark:bg-primary-container text-white">
                      <span className="material-symbols-outlined text-[18px]">workspace_premium</span>{t('courses.certificate')}
                    </button>
                  )}
                  <button onClick={() => setWatch(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                    <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
                  </button>
                </div>
              </div>
              {watch.description && <p className="text-sm text-on-surface dark:text-slate-300 mt-3 whitespace-pre-wrap leading-relaxed">{watch.description}</p>}

              {/* Q&A */}
              <div className="mt-6 border-t border-slate-100 dark:border-slate-800 pt-4">
                <h4 className="font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-[20px]">forum</span>{t('courses.qna')}
                </h4>
                <form onSubmit={submitComment} className="mb-4">
                  <textarea value={cText} onChange={e => setCText(e.target.value)} rows={2}
                    placeholder={cQuestion ? t('courses.askPlaceholder') : t('courses.commentPlaceholder')}
                    className="w-full px-3 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
                  <div className="flex items-center justify-between mt-2">
                    <label className="flex items-center gap-1.5 text-xs text-on-surface-variant dark:text-slate-400 cursor-pointer">
                      <input type="checkbox" checked={cQuestion} onChange={e => setCQuestion(e.target.checked)} className="w-3.5 h-3.5 accent-secondary-fixed" />
                      {t('courses.asQuestion')}
                    </label>
                    <button type="submit" disabled={cSending || !cText.trim()}
                      className="px-4 py-2 rounded-lg bg-primary dark:bg-primary-container text-white text-sm font-bold disabled:opacity-50">
                      {cSending ? t('courses.posting') : t('courses.post')}
                    </button>
                  </div>
                  {cQuestion && <p className="text-[11px] text-outline dark:text-slate-500 mt-1">{t('courses.aiHint')}</p>}
                </form>

                {cLoading ? (
                  <p className="text-sm text-outline dark:text-slate-500 text-center py-4">{t('courses.loading')}</p>
                ) : comments.length === 0 ? (
                  <p className="text-sm text-outline dark:text-slate-500 text-center py-4">{t('courses.noComments')}</p>
                ) : (
                  <ul className="space-y-3">
                    {comments.map(c => (
                      <li key={c.id} className={`${c.parentId || c.aiGenerated ? 'ml-6' : ''}`}>
                        <div className={`rounded-xl px-3 py-2.5 ${c.aiGenerated ? 'bg-secondary-container/30 dark:bg-secondary-fixed/10 border border-secondary-fixed/30' : 'bg-surface-container-low dark:bg-slate-800'}`}>
                          <div className="flex items-center gap-1.5 mb-1">
                            {c.aiGenerated && <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-[16px]">smart_toy</span>}
                            <span className={`text-xs font-bold ${c.aiGenerated ? 'text-secondary dark:text-secondary-fixed' : 'text-primary dark:text-white'}`}>{c.authorName}</span>
                            {c.staff && <span className="text-[9px] font-black px-1.5 py-0.5 rounded bg-primary/10 dark:bg-primary-container/30 text-primary dark:text-secondary-fixed">{t('courses.staffBadge')}</span>}
                            {c.question && <span className="text-[9px] font-black px-1.5 py-0.5 rounded bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300">Q</span>}
                            <span className="text-[10px] text-outline dark:text-slate-500 ml-auto">{new Date(c.createdAt).toLocaleDateString('ko', { month: 'short', day: 'numeric' })}</span>
                          </div>
                          <p className="text-sm text-on-surface dark:text-slate-200 whitespace-pre-wrap">{c.content}</p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 강좌 등록 모달 */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setShowForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('courses.register')}</h3>
              <button onClick={() => setShowForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-3">
              <p className="text-xs text-on-surface-variant dark:text-slate-400 bg-surface-container-low dark:bg-slate-800 rounded-xl px-3 py-2">{t('courses.registerHint')}</p>
              <input required value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder={t('courses.fTitle')} className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <input required value={form.videoUrl} onChange={e => setForm(f => ({ ...f, videoUrl: e.target.value }))} placeholder={t('courses.fVideo')} className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <div className="grid grid-cols-2 gap-3">
                <input value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} placeholder={t('courses.fCategory')} className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                <input value={form.thumbnailUrl} onChange={e => setForm(f => ({ ...f, thumbnailUrl: e.target.value }))} placeholder={t('courses.fThumb')} className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <textarea value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} rows={5} placeholder={t('courses.fDesc')} className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" disabled={saving || !form.title.trim() || !form.videoUrl.trim()} className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50">
                  {saving ? t('courses.saving') : t('courses.submit')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
