import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getProfOverview, getProfStudents, getProfStudentDetail, notifyProfStudent, notifyProfAtRisk, getProfAnalytics } from '../api/professor'
import { createNotice } from '../api/notice'

const RISK_STYLE = {
  HIGH:   'bg-error-container dark:bg-error/20 text-error',
  MEDIUM: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
  LOW:    'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed',
}

const gradeColor = (g) => {
  if (!g) return 'bg-surface-container dark:bg-slate-700 text-on-surface-variant dark:text-slate-300'
  if (g.startsWith('A')) return 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed'
  if (g.startsWith('B')) return 'bg-surface-container-highest dark:bg-slate-700 text-on-surface dark:text-white'
  return 'bg-error-container dark:bg-error/20 text-error'
}

export default function Professor() {
  const { t } = useTranslation()
  const [overview, setOverview] = useState(null)
  const [students, setStudents] = useState([])
  const [loading, setLoading]   = useState(true)
  const [selected, setSelected] = useState(null)   // detail
  const [selectedId, setSelectedId] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [search, setSearch]     = useState('')
  const [atRiskOnly, setAtRiskOnly] = useState(false)
  const [notifyTarget, setNotifyTarget] = useState(null) // {mode:'student'|'atrisk', student?}
  const [notifyForm, setNotifyForm] = useState({ title: '', message: '' })
  const [notifySending, setNotifySending] = useState(false)
  const [showNotice, setShowNotice] = useState(false)
  const [noticeForm, setNoticeForm] = useState({ title: '', summary: '', content: '', important: false })
  const [noticeSaving, setNoticeSaving] = useState(false)

  const handlePostNotice = async (e) => {
    e.preventDefault()
    if (!noticeForm.title.trim() || noticeSaving) return
    setNoticeSaving(true)
    try {
      await createNotice(noticeForm)
      setShowNotice(false)
      setNoticeForm({ title: '', summary: '', content: '', important: false })
      alert(t('professor.noticePosted'))
    } catch { alert(t('professor.noticeFailed')) }
    finally { setNoticeSaving(false) }
  }

  const filtered = students.filter(s => {
    if (atRiskOnly && !s.atRisk) return false
    const q = search.trim().toLowerCase()
    if (!q) return true
    return (s.name || '').toLowerCase().includes(q) || (s.studentId || '').toLowerCase().includes(q)
  })

  const openNotifyStudent = (s) => { setNotifyTarget({ mode: 'student', student: s }); setNotifyForm({ title: '', message: '' }) }
  const openNotifyAtRisk  = () => { setNotifyTarget({ mode: 'atrisk' }); setNotifyForm({ title: '', message: '' }) }
  const handleSendNotify = async (e) => {
    e.preventDefault()
    if (!notifyForm.message.trim() || notifySending) return
    setNotifySending(true)
    try {
      if (notifyTarget.mode === 'student') {
        await notifyProfStudent(notifyTarget.student.id, notifyForm)
        alert(t('professor.notifySent', { name: notifyTarget.student.name }))
      } else {
        const r = await notifyProfAtRisk(notifyForm)
        alert(t('professor.notifyAtRiskSent', { count: r.data?.notified ?? 0 }))
      }
      setNotifyTarget(null)
    } catch { alert(t('professor.notifyFailed')) }
    finally { setNotifySending(false) }
  }

  const [view, setView] = useState('students')   // students | analytics
  const [analytics, setAnalytics] = useState(null)

  useEffect(() => {
    Promise.all([
      getProfOverview().then(r => setOverview(r.data)).catch(() => {}),
      getProfStudents().then(r => setStudents(r.data ?? [])).catch(() => setStudents([])),
      getProfAnalytics().then(r => setAnalytics(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false))
  }, [])

  const openDetail = async (id) => {
    setDetailLoading(true)
    setSelectedId(id)
    setSelected({})
    try { const r = await getProfStudentDetail(id); setSelected(r.data) }
    catch { setSelected(null) }
    finally { setDetailLoading(false) }
  }

  return (
    <Layout title={t('professor.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('professor.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('professor.subtitle')}</p>
      </div>

      {/* 개요 카드 */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="card p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('professor.studentCount')}</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">groups</span>
          </div>
          <p className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">{overview?.studentCount ?? '—'}</p>
        </div>
        <div className="card p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('professor.avgGpa')}</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">military_tech</span>
          </div>
          <p className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">{overview?.avgGpa?.toFixed(2) ?? '—'}</p>
        </div>
        <div className="card p-5">
          <div className="flex justify-between items-center mb-2">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('professor.atRisk')}</span>
            <span className="material-symbols-outlined text-error">warning</span>
          </div>
          <p className="text-3xl font-black text-error font-['Space_Grotesk']">{overview?.atRiskCount ?? '—'}</p>
        </div>
      </div>

      {/* 뷰 탭 */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-5">
        {[['students', t('professor.tabStudents'), 'group'], ['analytics', t('professor.tabAnalytics'), 'insights']].map(([k, label, icon]) => (
          <button key={k} onClick={() => setView(k)}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-bold transition-all ${view === k ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm' : 'text-on-surface-variant dark:text-slate-400'}`}>
            <span className="material-symbols-outlined text-[18px]">{icon}</span>{label}
          </button>
        ))}
      </div>

      {view === 'students' && (<>
      {/* 검색 / 필터 / 위험군 알림 */}
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <div className="relative flex-1 min-w-[180px]">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline dark:text-slate-500 text-[18px]">search</span>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder={t('professor.searchPlaceholder')}
            className="w-full pl-9 pr-3 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
        </div>
        <button onClick={() => setAtRiskOnly(v => !v)}
          className={`px-4 py-2.5 rounded-xl text-sm font-bold transition-colors shrink-0 ${atRiskOnly ? 'bg-error text-white' : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300'}`}>
          {t('professor.filterAtRisk')}
        </button>
        <button onClick={openNotifyAtRisk}
          className="px-4 py-2.5 rounded-xl text-sm font-bold bg-primary dark:bg-primary-container text-white shrink-0 flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[18px]">campaign</span>{t('professor.notifyAtRisk')}
        </button>
        <button onClick={() => setShowNotice(true)}
          className="px-4 py-2.5 rounded-xl text-sm font-bold bg-secondary-fixed text-primary shrink-0 flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[18px]">edit_note</span>{t('professor.postNotice')}
        </button>
      </div>

      {/* 학생 목록 */}
      <div className="card overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
        ) : filtered.length === 0 ? (
          <p className="py-16 text-center text-on-surface-variant dark:text-slate-400">{t('professor.noStudents')}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 dark:border-slate-800 text-label-md text-outline dark:text-slate-500">
                  <th className="py-3 px-4">{t('professor.colName')}</th>
                  <th className="py-3 px-4">{t('professor.colId')}</th>
                  <th className="py-3 px-4">{t('professor.colGrade')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colGpa')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colAttend')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colStatus')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                {filtered.map(s => (
                  <tr key={s.id} onClick={() => openDetail(s.id)}
                    className="hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer transition-colors">
                    <td className="py-3 px-4 font-bold text-primary dark:text-white">{s.name}</td>
                    <td className="py-3 px-4 font-mono text-outline dark:text-slate-400">{s.studentId}</td>
                    <td className="py-3 px-4 text-on-surface-variant dark:text-slate-300">{t('professor.gradeSemester', { grade: s.grade, semester: s.semester })}</td>
                    <td className="py-3 px-4 text-center font-bold text-primary dark:text-secondary-fixed">{s.gpa ? s.gpa.toFixed(2) : '—'}</td>
                    <td className="py-3 px-4 text-center">{s.attendanceRate != null ? `${s.attendanceRate}%` : '—'}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${s.atRisk ? 'bg-error-container dark:bg-error/20 text-error' : 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed'}`}>
                        {s.atRisk ? t('professor.statusRisk') : t('professor.statusOk')}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      </>)}

      {/* ── 학습 분석 대시보드 ── */}
      {view === 'analytics' && analytics && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
            <div className="card p-4"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.aHighRisk')}</p><p className="text-2xl font-black text-error font-['Space_Grotesk']">{analytics.aggregate.highRisk}</p></div>
            <div className="card p-4"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.aMedRisk')}</p><p className="text-2xl font-black text-yellow-600 dark:text-yellow-400 font-['Space_Grotesk']">{analytics.aggregate.mediumRisk}</p></div>
            <div className="card p-4"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.aAvgAttend')}</p><p className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">{analytics.aggregate.avgAttendance != null ? analytics.aggregate.avgAttendance + '%' : '—'}</p></div>
            <div className="card p-4"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.aCompletions')}</p><p className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">{analytics.aggregate.courseCompletions}</p></div>
          </div>
          <div className="flex items-center justify-between mb-3 gap-2">
            <p className="text-xs text-outline dark:text-slate-500">{t('professor.analyticsHint')}</p>
            <button onClick={openNotifyAtRisk} className="px-3 py-2 rounded-xl text-sm font-bold bg-primary dark:bg-primary-container text-white shrink-0 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px]">campaign</span>{t('professor.notifyAtRisk')}
            </button>
          </div>
          <div className="card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead><tr className="border-b border-slate-100 dark:border-slate-800 text-label-md text-outline dark:text-slate-500">
                  <th className="py-3 px-4">{t('professor.colName')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colRisk')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colGpa')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colAttend')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colCompletions')}</th>
                  <th className="py-3 px-4 text-center">{t('professor.colQuiz')}</th>
                  <th className="py-3 px-4">{t('professor.colReasons')}</th>
                </tr></thead>
                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                  {analytics.students.map(s => (
                    <tr key={s.id} onClick={() => openDetail(s.id)} className="hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer transition-colors">
                      <td className="py-3 px-4 font-bold text-primary dark:text-white">{s.name}<span className="block text-[11px] font-mono font-normal text-outline dark:text-slate-500">{s.studentId}</span></td>
                      <td className="py-3 px-4 text-center"><span className={`text-[11px] font-black px-2.5 py-1 rounded-full ${RISK_STYLE[s.riskLevel]}`}>{t('professor.risk_' + s.riskLevel)} {s.riskScore}</span></td>
                      <td className="py-3 px-4 text-center font-bold text-primary dark:text-secondary-fixed">{s.gpa ? s.gpa.toFixed(2) : '—'}</td>
                      <td className="py-3 px-4 text-center">{s.attendanceRate != null ? `${s.attendanceRate}%` : '—'}</td>
                      <td className="py-3 px-4 text-center">{s.courseCompletions}</td>
                      <td className="py-3 px-4 text-center">{s.quizAvg != null ? `${s.quizAvg}%` : '—'}</td>
                      <td className="py-3 px-4">
                        <span className="flex flex-wrap gap-1">
                          {s.riskReasons.map(r => <span key={r} className="text-[10px] px-1.5 py-0.5 rounded bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400">{t('professor.risk.' + r)}</span>)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* 학생 상세 모달 */}
      {selected && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setSelected(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-2xl shadow-2xl max-h-[90vh] overflow-y-auto">
            {detailLoading || !selected.studentId ? (
              <div className="flex justify-center py-20"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
            ) : (
              <>
                <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between sticky top-0 bg-white dark:bg-slate-900">
                  <div>
                    <h3 className="font-['Space_Grotesk'] text-xl font-bold text-primary dark:text-white">{selected.name}</h3>
                    <p className="text-sm text-outline dark:text-slate-400 mt-0.5">
                      {selected.studentId} · {t('professor.gradeSemester', { grade: selected.grade, semester: selected.semester })} · {selected.department}
                    </p>
                    {(selected.phone || selected.email) && (
                      <p className="text-xs text-outline dark:text-slate-500 mt-1">{[selected.phone, selected.email].filter(Boolean).join(' · ')}</p>
                    )}
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <button onClick={() => openNotifyStudent({ id: selectedId, name: selected.name })}
                      className="px-3 py-2 rounded-xl bg-primary dark:bg-primary-container text-white text-sm font-bold flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[18px]">send</span>{t('professor.notify')}
                    </button>
                    <button onClick={() => setSelected(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                      <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
                    </button>
                  </div>
                </div>

                <div className="p-6 space-y-6">
                  {/* 요약 */}
                  <div className="grid grid-cols-3 gap-3">
                    <div className="card p-3 text-center"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.colGpa')}</p><p className="text-2xl font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk']">{selected.gpa?.toFixed(2) ?? '—'}</p></div>
                    <div className="card p-3 text-center"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.credits')}</p><p className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">{selected.totalCredits ?? 0}</p></div>
                    <div className="card p-3 text-center"><p className="text-label-md text-outline dark:text-slate-400">{t('professor.rate')}</p><p className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">{selected.attendance?.rate != null ? `${selected.attendance.rate}%` : '—'}</p></div>
                  </div>

                  {/* 출결 경고 */}
                  {selected.attendance?.warnings?.length > 0 && (
                    <div className="p-3 bg-error-container dark:bg-error/20 text-error rounded-xl flex items-center gap-2 text-sm font-bold">
                      <span className="material-symbols-outlined text-[18px]">warning</span>
                      {t('professor.warning')}: {selected.attendance.warnings.join(', ')}
                    </div>
                  )}

                  {/* 출결 요약 */}
                  <div className="grid grid-cols-4 gap-2 text-center">
                    {[['present', selected.attendance?.present], ['late', selected.attendance?.late], ['absent', selected.attendance?.absent], ['excused', selected.attendance?.excused]].map(([k, v]) => (
                      <div key={k} className="bg-surface-container-low dark:bg-slate-800 rounded-xl py-2">
                        <p className="text-label-md text-outline dark:text-slate-400">{t(`professor.${k}`)}</p>
                        <p className={`text-lg font-black ${k === 'absent' && v > 0 ? 'text-error' : 'text-on-surface dark:text-white'}`}>{v ?? 0}</p>
                      </div>
                    ))}
                  </div>

                  {/* 성적 */}
                  <div>
                    <p className="font-bold text-primary dark:text-white mb-2">{t('professor.grades')}</p>
                    {(selected.grades ?? []).length === 0 ? (
                      <p className="text-sm text-outline dark:text-slate-500 py-4 text-center">{t('common.noData')}</p>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                          <thead><tr className="text-label-md text-outline dark:text-slate-500 border-b border-slate-100 dark:border-slate-800">
                            <th className="py-2 px-3">{t('professor.subject')}</th><th className="py-2 px-3 text-center">{t('professor.term')}</th><th className="py-2 px-3 text-center">{t('professor.credit')}</th><th className="py-2 px-3 text-center">{t('professor.colGpa')}</th>
                          </tr></thead>
                          <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                            {selected.grades.map((g, i) => (
                              <tr key={i}>
                                <td className="py-2 px-3 font-medium text-on-surface dark:text-white">{g.subjectName}</td>
                                <td className="py-2 px-3 text-center text-outline dark:text-slate-400">{g.gradeYear}-{g.gradeSemester}</td>
                                <td className="py-2 px-3 text-center text-on-surface dark:text-slate-300">{g.credits}</td>
                                <td className="py-2 px-3 text-center"><span className={`font-black px-2.5 py-0.5 rounded-full text-xs ${gradeColor(g.letterGrade)}`}>{g.letterGrade}</span></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* 알림 발송 모달 */}
      {notifyTarget && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setNotifyTarget(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">
                {notifyTarget.mode === 'student'
                  ? t('professor.notifyTo', { name: notifyTarget.student?.name })
                  : t('professor.notifyAtRiskTitle')}
              </h3>
              <button onClick={() => setNotifyTarget(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSendNotify} className="p-6 space-y-3">
              {notifyTarget.mode === 'atrisk' && (
                <p className="text-xs text-on-surface-variant dark:text-slate-400 bg-surface-container-low dark:bg-slate-800 rounded-xl px-3 py-2">{t('professor.notifyAtRiskHint')}</p>
              )}
              <input value={notifyForm.title} onChange={e => setNotifyForm(f => ({ ...f, title: e.target.value }))}
                placeholder={t('professor.notifyTitlePlaceholder')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <textarea required value={notifyForm.message} onChange={e => setNotifyForm(f => ({ ...f, message: e.target.value }))}
                rows={5} placeholder={t('professor.notifyMessagePlaceholder')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setNotifyTarget(null)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" disabled={notifySending || !notifyForm.message.trim()}
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50">
                  {notifySending ? t('professor.notifySending') : t('professor.notifySend')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 공지 작성 모달 (전체 학생 알림+푸시 broadcast) */}
      {showNotice && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setShowNotice(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('professor.postNotice')}</h3>
              <button onClick={() => setShowNotice(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handlePostNotice} className="p-6 space-y-3">
              <p className="text-xs text-on-surface-variant dark:text-slate-400 bg-surface-container-low dark:bg-slate-800 rounded-xl px-3 py-2">{t('professor.noticeHint')}</p>
              <input required value={noticeForm.title} onChange={e => setNoticeForm(f => ({ ...f, title: e.target.value }))}
                placeholder={t('professor.noticeTitle')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <input value={noticeForm.summary} onChange={e => setNoticeForm(f => ({ ...f, summary: e.target.value }))}
                placeholder={t('professor.noticeSummary')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <textarea value={noticeForm.content} onChange={e => setNoticeForm(f => ({ ...f, content: e.target.value }))}
                rows={6} placeholder={t('professor.noticeContent')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={noticeForm.important} onChange={e => setNoticeForm(f => ({ ...f, important: e.target.checked }))}
                  className="w-4 h-4 accent-secondary-fixed" />
                <span className="text-sm text-on-surface dark:text-slate-200">{t('professor.noticeImportant')}</span>
              </label>
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowNotice(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" disabled={noticeSaving || !noticeForm.title.trim()}
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50">
                  {noticeSaving ? t('professor.noticePosting') : t('professor.noticePost')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
