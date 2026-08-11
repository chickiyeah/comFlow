import { useState, useEffect } from 'react'
import { useTranslation, Trans } from 'react-i18next'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line, CartesianGrid } from 'recharts'
import Layout from '../components/layout/Layout'
import { getMyGrades, downloadTranscriptPdf } from '../api/grade'
import { getMyAttendance } from '../api/attendance'
import { checkGraduation } from '../api/graduation'
import { getMyAwards, createAward, deleteAward } from '../api/award'
import { getPortalGradeTerms, getPortalGradeDetail, getPortalAttendance, getPortalSchedule } from '../api/portal'
import { getGradeTrend, getAttendanceSummary } from '../api/analytics'
import { getMyReviews, createReview, deleteReview } from '../api/review'
import { getProfile } from '../api/profile'
import { getCurrentTerm, recentYears } from '../utils/term'
import AiStudyPanel from '../components/academic/AiStudyPanel'

const AWARD_LEVELS = ['GOLD','SILVER','BRONZE','ENCOURAGEMENT','PARTICIPATION']
const AWARD_LEVEL_I18N = {
  GOLD:'academic.awardLevelGold', SILVER:'academic.awardLevelSilver', BRONZE:'academic.awardLevelBronze',
  ENCOURAGEMENT:'academic.awardLevelEncouragement', PARTICIPATION:'academic.awardLevelParticipation',
}
const AWARD_LEVEL_COLOR = {
  GOLD:          'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400',
  SILVER:        'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300',
  BRONZE:        'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400',
  ENCOURAGEMENT: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400',
  PARTICIPATION: 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400',
}

function AttendanceBar({ status }) {
  if (!status) return <div className="w-full h-8 bg-surface-container dark:bg-slate-800 rounded-lg border border-dashed border-outline dark:border-slate-700" />
  const color = status === 'P' ? 'bg-secondary dark:bg-secondary-fixed/80' : status === 'L' ? 'bg-surface-tint' : 'bg-error'
  return <div className={`w-full h-8 ${color} rounded-lg`} />
}

const SCHED_DAYS = [
  { key: 'W_MON_SBJT_CD', i18n: 'academic.dayMon' },
  { key: 'W_TUE_SBJT_CD', i18n: 'academic.dayTue' },
  { key: 'W_WED_SBJT_CD', i18n: 'academic.dayWed' },
  { key: 'W_THU_SBJT_CD', i18n: 'academic.dayThu' },
  { key: 'W_FRI_SBJT_CD', i18n: 'academic.dayFri' },
]
const SCHED_COLORS = [
  'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-200',
  'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-200',
  'bg-purple-100 dark:bg-purple-900/40 text-purple-800 dark:text-purple-200',
  'bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-200',
  'bg-pink-100 dark:bg-pink-900/40 text-pink-800 dark:text-pink-200',
  'bg-teal-100 dark:bg-teal-900/40 text-teal-800 dark:text-teal-200',
  'bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-200',
]

function parseCell(v) {
  if (!v) return null
  const parts = v.split('\n\r').map(s => s.trim().replace(/^\(|\)$/g, '').trim()).filter(Boolean)
  return { name: parts[0] ?? '', prof: parts[1] ?? '', room: parts[2] ?? '' }
}

function ScheduleGrid({ data }) {
  const { t } = useTranslation()
  const rows = data.filter(r => SCHED_DAYS.some(d => r[d.key]))
  const colorMap = {}
  let ci = 0
  rows.forEach(r => SCHED_DAYS.forEach(d => {
    const c = parseCell(r[d.key])
    if (c && !colorMap[c.name]) colorMap[c.name] = SCHED_COLORS[ci++ % SCHED_COLORS.length]
  }))
  if (!rows.length) return <p className="text-center py-8 text-outline dark:text-slate-400">{t('academic.noScheduleData')}</p>
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="bg-surface-container dark:bg-slate-800">
            <th className="border border-slate-200 dark:border-slate-700 px-3 py-2 text-outline dark:text-slate-400 font-bold text-xs w-20 text-center">{t('academic.period')}</th>
            {SCHED_DAYS.map(d => (
              <th key={d.key} className="border border-slate-200 dark:border-slate-700 px-3 py-2 text-primary dark:text-white font-bold text-center min-w-[110px]">{t(d.i18n)}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, ri) => (
            <tr key={ri} className="hover:bg-surface-container-low dark:hover:bg-slate-800/50 transition-colors">
              <td className="border border-slate-200 dark:border-slate-700 px-2 py-2 text-center text-xs text-outline dark:text-slate-400 whitespace-nowrap">
                <div className="font-bold">{row.CD_NM}</div>
                <div>{(row.NM_TIME ?? '').split('\n\r').find(s => s.includes(':'))?.trim()}</div>
              </td>
              {SCHED_DAYS.map(d => {
                const c = parseCell(row[d.key])
                return (
                  <td key={d.key} className="border border-slate-200 dark:border-slate-700 px-1.5 py-1.5">
                    {c ? (
                      <div className={`rounded-lg px-2 py-1.5 text-center ${colorMap[c.name] ?? SCHED_COLORS[0]}`}>
                        <div className="font-bold text-xs leading-tight">{c.name}</div>
                        {c.prof && <div className="text-[10px] opacity-80 mt-0.5">{c.prof}</div>}
                        {c.room && <div className="text-[10px] opacity-70">{c.room}</div>}
                      </div>
                    ) : null}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function PasswordModal({ title, onConfirm, onClose, loading }) {
  const { t } = useTranslation()
  const [pw, setPw] = useState('')
  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
      onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-sm shadow-2xl p-6">
        <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-2 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary-fixed">lock</span>{title}
        </h3>
        <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-4">
          {t('academic.portalPwDesc')}
        </p>
        <input
          type="password" value={pw} onChange={e => setPw(e.target.value)}
          placeholder={t('academic.portalPwPlaceholder')}
          className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm mb-4 focus:outline-none focus:ring-2 focus:ring-primary/30"
          onKeyDown={e => e.key === 'Enter' && pw && onConfirm(pw)}
          autoFocus
        />
        <div className="flex gap-3">
          <button onClick={onClose} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
          <button onClick={() => pw && onConfirm(pw)} disabled={!pw || loading}
            className="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold disabled:opacity-50 flex items-center justify-center gap-2">
            {loading && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
            {t('academic.lookup')}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Academic() {
  const { t } = useTranslation()
  const ALL_TABS = [
    { key: 'grade',           icon: 'grade',             i18n: 'academic.tabGrade'          },
    { key: 'portal_grade',    icon: 'history_edu',       i18n: 'academic.tabPortalGrade'    },
    { key: 'attendance',      icon: 'calendar_today',    i18n: 'academic.tabAttendance'     },
    { key: 'portal_attend',   icon: 'fact_check',        i18n: 'academic.tabPortalAttend'   },
    { key: 'portal_schedule', icon: 'event_note',        i18n: 'academic.tabPortalSchedule' },
    { key: 'graduation',      icon: 'school',            i18n: 'academic.tabGraduation'     },
    { key: 'analysis',        icon: 'bar_chart',         i18n: 'academic.tabAnalysis'       },
    { key: 'planner',         icon: 'auto_awesome',      i18n: 'academic.tabPlanner'        },
    { key: 'reviews',         icon: 'rate_review',       i18n: 'academic.tabReviews'        },
    { key: 'awards',          icon: 'workspace_premium', i18n: 'academic.tabAwards'         },
  ]
  const PORTAL_TAB_KEYS = ['portal_grade', 'portal_attend', 'portal_schedule']

  const [tab, setTab]           = useState('grade')
  const [grades, setGrades]     = useState(null)
  const [attendance, setAttendance] = useState(null)
  const [graduation, setGraduation] = useState(null)
  const [awards, setAwards]     = useState([])
  const [loading, setLoading]   = useState(false)

  // 포털 성적
  const [portalTerms, setPortalTerms]       = useState([])
  const [selectedTerm, setSelectedTerm]     = useState(null)
  const [portalDetail, setPortalDetail]     = useState([])
  const [termLoading, setTermLoading]       = useState(false)

  // 포털 시간표
  const [portalSchedule, setPortalSchedule]   = useState(null)
  const [scheduleYear, setScheduleYear]       = useState(String(getCurrentTerm().year))
  const [scheduleSemester, setScheduleSemester] = useState(getCurrentTerm().smrCode)
  const [scheduleLoading, setScheduleLoading] = useState(false)

  // 포털 출결
  const [portalAttend, setPortalAttend]     = useState(null)
  const [showPwModal, setShowPwModal]       = useState(false)
  const [attendLoading, setAttendLoading]   = useState(false)
  const [hasPortalPw, setHasPortalPw]         = useState(false)
  const [portalSynced, setPortalSynced]       = useState(false)   // intranetSyncEnabled
  const [transcriptLoading, setTranscriptLoading] = useState(false)

  const handleTranscript = async () => {
    if (transcriptLoading) return
    setTranscriptLoading(true)
    try {
      const blob = await downloadTranscriptPdf()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = '성적증명서.pdf'
      document.body.appendChild(a); a.click(); a.remove()
      URL.revokeObjectURL(url)
    } catch {
      alert(t('academic.transcriptError'))
    } finally {
      setTranscriptLoading(false)
    }
  }

  // 마운트 시 포털 연동 상태 확인
  useEffect(() => {
    getProfile().then(r => {
      if (r.data?.hasPortalPassword) setHasPortalPw(true)
      if (r.data?.intranetSyncEnabled) setPortalSynced(true)
    }).catch(() => {})
  }, [])

  // 포털 미연동 시 포털 의존 탭은 렌더링하지 않는다
  const TABS = portalSynced ? ALL_TABS : ALL_TABS.filter(tb => !PORTAL_TAB_KEYS.includes(tb.key))

  // 학습 분석
  const [gradeTrend, setGradeTrend]         = useState([])
  const [attendSummary, setAttendSummary]   = useState(null)

  // 강의 리뷰
  const [reviews, setReviews]               = useState([])
  const [reviewForm, setReviewForm]         = useState({ subjectName:'', rating:5, anonymous:false, content:'' })
  const [showReviewForm, setShowReviewForm] = useState(false)

  // 수상내역
  const [showAwardForm, setShowAwardForm]   = useState(false)
  const [awardForm, setAwardForm]           = useState({ title:'', organization:'', level:'GOLD', awardDate:'', description:'' })

  const loadAwards  = () => getMyAwards().then(r => setAwards(r.data ?? [])).catch(() => setAwards([]))
  const loadReviews = () => getMyReviews().then(r => setReviews(r.data ?? [])).catch(() => setReviews([]))

  useEffect(() => {
    setLoading(true)
    if (tab === 'grade') {
      getMyGrades().then(r => setGrades(r.data)).catch(() => setGrades(null)).finally(() => setLoading(false))
    } else if (tab === 'attendance') {
      getMyAttendance().then(r => setAttendance(r.data)).catch(() => setAttendance(null)).finally(() => setLoading(false))
    } else if (tab === 'awards') {
      loadAwards().finally(() => setLoading(false))
    } else if (tab === 'graduation') {
      checkGraduation().then(r => setGraduation(r.data)).catch(() => setGraduation(null)).finally(() => setLoading(false))
    } else if (tab === 'portal_grade') {
      setLoading(false)
      if (!portalTerms.length) {
        setTermLoading(true)
        getPortalGradeTerms()
          .then(r => { const terms = r.data ?? []; console.log('[포털성적] terms[0]:', terms[0]); setPortalTerms(terms); if (terms.length) setSelectedTerm(terms[0]) })
          .catch(() => setPortalTerms([]))
          .finally(() => setTermLoading(false))
      }
    } else if (tab === 'portal_schedule') {
      setLoading(false)
      if (!portalSchedule) {
        setScheduleLoading(true)
        getPortalSchedule(scheduleYear, scheduleSemester)
          .then(r => setPortalSchedule(r.data ?? []))
          .catch(() => setPortalSchedule([]))
          .finally(() => setScheduleLoading(false))
      }
    } else if (tab === 'portal_attend') {
      setLoading(false)
      if (!portalAttend) {
        if (hasPortalPw) {
          // 저장된 비밀번호로 자동 조회 (빈 문자열 전송 → 서버가 저장된 비밀번호 사용)
          setAttendLoading(true)
          getPortalAttendance('').then(r => setPortalAttend(r.data))
            .catch(() => setShowPwModal(true))  // 저장된 비밀번호 실패 시 모달
            .finally(() => setAttendLoading(false))
        } else {
          setShowPwModal(true)
        }
      }
    } else if (tab === 'analysis') {
      Promise.all([
        getGradeTrend().then(r => setGradeTrend(r.data ?? [])).catch(() => {}),
        getAttendanceSummary().then(r => setAttendSummary(r.data)).catch(() => {}),
      ]).finally(() => setLoading(false))
    } else if (tab === 'reviews') {
      loadReviews().finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [tab])

  useEffect(() => {
    if (!selectedTerm) return
    setTermLoading(true)
    const meta = {
      smrNm:       selectedTerm.SMR_NM       ?? selectedTerm.smr_nm       ?? '',
      gpaAvg:      selectedTerm.GPA_AVG      ?? selectedTerm.gpa_avg      ?? '0',
      sumAcqPoint: selectedTerm.SUM_ACQ_POINT ?? selectedTerm.sum_acq_point ?? '0',
      sumFacPoint: selectedTerm.SUM_FAC_POINT ?? selectedTerm.sum_fac_point ?? '0',
      cntAtlecSbjt:selectedTerm.CNT_ATLEC_SBJT ?? selectedTerm.cnt_atlec_sbjt ?? '0',
      cntEvlSbjt:  selectedTerm.CNT_EVL_SBJT  ?? selectedTerm.cnt_evl_sbjt  ?? '0',
      pergAvg:     selectedTerm.PERG_AVG      ?? selectedTerm.perg_avg      ?? '0',
      percPnt:     selectedTerm.PERC_PNT      ?? selectedTerm.perc_pnt      ?? '0',
      bachWarnCnt: selectedTerm.BACH_WARN_CNT ?? selectedTerm.bach_warn_cnt ?? '0',
    }
    const yr  = selectedTerm.YEAR ?? selectedTerm.year
    const smr = selectedTerm.SMR  ?? selectedTerm.smr
    getPortalGradeDetail(yr, smr, meta)
      .then(r => setPortalDetail(r.data ?? []))
      .catch(() => setPortalDetail([]))
      .finally(() => setTermLoading(false))
  }, [selectedTerm])

  const handlePortalAttend = async (pw) => {
    setAttendLoading(true)
    try {
      const r = await getPortalAttendance(pw)
      setPortalAttend(r.data)
      setShowPwModal(false)
      setHasPortalPw(true)
    } catch {
      alert(t('academic.attendLookupFailed'))
    } finally {
      setAttendLoading(false)
    }
  }

  const gradeColor = (g) => {
    if (!g) return ''
    if (g.startsWith('A')) return 'bg-secondary-container dark:bg-secondary-fixed text-on-secondary-container dark:text-[#131f00]'
    if (g.startsWith('B')) return 'bg-surface-container-highest dark:bg-slate-700 text-on-surface dark:text-white'
    return 'bg-error-container dark:bg-error/20 text-error'
  }

  const smrLabel = (smr) => smr === 'SU002001' ? t('academic.semester1') : smr === 'SU002002' ? t('academic.semester2') : smr ?? ''

  return (
    <Layout title={t('academic.layoutTitle')}>
      <div className="mb-6 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('academic.title')}</h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('academic.subtitle')}</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleTranscript} disabled={transcriptLoading} className="btn-primary text-sm disabled:opacity-60">
            <span className="material-symbols-outlined text-[18px]">download</span>{t('academic.issueCertificate')}
          </button>
          <button onClick={handleTranscript} disabled={transcriptLoading} className="btn-secondary text-sm disabled:opacity-60">
            <span className="material-symbols-outlined text-[18px]">print</span>{t('academic.transcript')}
          </button>
        </div>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('academic.gpa')}</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">military_tech</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">
              {grades?.gpa?.toFixed(2) ?? '4.2'}
            </span>
            <span className="text-on-surface-variant dark:text-slate-400">/ 4.5</span>
          </div>
        </div>
        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('academic.attendance')}</span>
            <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">task_alt</span>
          </div>
          <div className="text-3xl font-black text-secondary dark:text-white font-['Space_Grotesk']">
            {attendance ? `${Math.round((attendance.totalPresent / Math.max(attendance.totalPresent + attendance.totalAbsent + attendance.totalLate, 1)) * 100)}%` : '—'}
          </div>
        </div>
        {portalSynced && (
        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('academic.portalAttendRate')}</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">fact_check</span>
          </div>
          <div className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">
            {portalAttend?.summary?.attendance_rate != null ? `${portalAttend.summary.attendance_rate}%` : '—'}
          </div>
          {!portalAttend && <p className="text-[11px] text-outline dark:text-slate-500 mt-1">{t('academic.checkInPortalAttendTab')}</p>}
        </div>
        )}
        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">{t('academic.earnedCredits')}</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">school</span>
          </div>
          <div className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">
            {graduation?.totalEarnedCredits ?? '—'}
          </div>
          {graduation && <p className="text-[11px] text-outline dark:text-slate-500 mt-1">{t('academic.creditsRequired', { count: graduation.requiredTotalCredits })}</p>}
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 overflow-hidden">
        <div className="flex border-b border-slate-100 dark:border-slate-800 px-2 overflow-x-auto">
          {TABS.map(tb => (
            <button key={tb.key} onClick={() => setTab(tb.key)}
              className={`flex items-center gap-2 px-4 py-4 text-sm font-bold border-b-2 shrink-0 transition-colors ${
                tab === tb.key
                  ? 'border-primary dark:border-secondary-fixed text-primary dark:text-secondary-fixed'
                  : 'border-transparent text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
              }`}>
              <span className="material-symbols-outlined text-[18px]">{tb.icon}</span>
              <span className="hidden md:inline">{t(tb.i18n)}</span>
            </button>
          ))}
        </div>

        <div className="p-4 lg:p-6">
          {loading && (
            <div className="flex justify-center py-12">
              <div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
            </div>
          )}

          {/* ── 성적 탭 ── */}
          {!loading && tab === 'grade' && (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-slate-100 dark:border-slate-800">
                    {[t('academic.colSubject'),t('academic.colCategory'),t('academic.colCredits'),t('academic.colScore'),t('academic.colGrade')].map((h, i) => (
                      <th key={i} className={`py-3 px-3 text-label-md text-outline dark:text-slate-500 uppercase tracking-wider ${i >= 2 ? 'text-center' : ''}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                  {(grades?.grades ?? []).length === 0 ? (
                    <tr><td colSpan={5} className="py-12 text-center text-on-surface-variant dark:text-slate-400">{portalSynced ? t('academic.noGradeDataUsePortal') : t('academic.noGradeData')}</td></tr>
                  ) : (grades?.grades ?? []).map((g, i) => (
                    <tr key={i} className="hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors">
                      <td className="py-4 px-3">
                        <div className="font-bold text-primary dark:text-white">{g.subjectName}</div>
                        <div className="text-[11px] text-outline dark:text-slate-500">{g.subjectCode}</div>
                      </td>
                      <td className="py-4 px-3">
                        <span className="bg-surface-container dark:bg-slate-700 text-on-surface-variant dark:text-slate-300 px-2 py-1 rounded text-[11px]">{t('academic.major')}</span>
                      </td>
                      <td className="py-4 px-3 text-center font-medium text-on-surface dark:text-white">{g.credits}</td>
                      <td className="py-4 px-3 text-center font-bold text-primary dark:text-secondary-fixed">{Math.round((g.gradePoint ?? 0) * 20)}</td>
                      <td className="py-4 px-3 text-center">
                        <span className={`font-black px-3 py-1 rounded-full text-xs ${gradeColor(g.letterGrade)}`}>{g.letterGrade}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* ── 포털 성적 탭 ── */}
          {tab === 'portal_grade' && (
            <div>
              {termLoading && !portalTerms.length ? (
                <div className="flex justify-center py-12"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
              ) : !portalTerms.length ? (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">history_edu</span>
                  {portalSynced ? (
                    <>
                      <p className="mt-3 font-bold text-primary dark:text-white">{t('academic.portalSessionExpired')}</p>
                      <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1"><Trans i18nKey="academic.portalResyncHint" components={{ strong: <strong /> }} /></p>
                    </>
                  ) : (
                    <>
                      <p className="mt-3 font-bold text-primary dark:text-white">{t('academic.portalLinkRequired')}</p>
                      <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('academic.portalLinkFirst')}</p>
                    </>
                  )}
                </div>
              ) : (
                <>
                  {/* 학기 선택 */}
                  <div className="flex gap-2 flex-wrap mb-6">
                    {portalTerms.map((term, i) => (
                      <button key={i}
                        onClick={() => setSelectedTerm(term)}
                        className={`px-4 py-2 rounded-xl text-sm font-bold transition-colors ${
                          selectedTerm === term
                            ? 'bg-primary text-white dark:bg-secondary-fixed dark:text-[#131f00]'
                            : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:bg-primary/10'
                        }`}>
                        {t('academic.yearLabel', { year: term.YEAR ?? term.year })} {smrLabel(term.SMR ?? term.smr)}
                        {(term.GPA_AVG ?? term.gpa_avg) && <span className="ml-2 opacity-70">GPA {parseFloat(term.GPA_AVG ?? term.gpa_avg).toFixed(2)}</span>}
                      </button>
                    ))}
                  </div>

                  {/* 선택 학기 요약 */}
                  {selectedTerm && (
                    <div className="grid grid-cols-3 gap-3 mb-6">
                      {[
                        { label: 'GPA', val: (selectedTerm.GPA_AVG ?? selectedTerm.gpa_avg) ? parseFloat(selectedTerm.GPA_AVG ?? selectedTerm.gpa_avg).toFixed(2) : '—' },
                        { label: t('academic.acquiredCredits'), val: selectedTerm.SUM_ACQ_POINT ?? selectedTerm.sum_acq_point ?? '—' },
                        { label: t('academic.coursesTaken'), val: selectedTerm.CNT_ATLEC_SBJT ?? selectedTerm.cnt_atlec_sbjt ?? '—' },
                      ].map(s => (
                        <div key={s.label} className="card p-4 text-center">
                          <p className="text-label-md text-outline dark:text-slate-400 mb-1">{s.label}</p>
                          <p className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">{s.val}</p>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* 과목별 성적 */}
                  {termLoading ? (
                    <div className="flex justify-center py-8"><div className="w-6 h-6 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left">
                        <thead>
                          <tr className="border-b border-slate-100 dark:border-slate-800">
                            {[t('academic.colSubject'),t('academic.colCredits'),t('academic.colGrade'),t('academic.colAvgPoint')].map((h, i) => (
                              <th key={i} className={`py-3 px-3 text-label-md text-outline dark:text-slate-500 uppercase ${i >= 1 ? 'text-center' : ''}`}>{h}</th>
                            ))}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                          {portalDetail.length === 0 ? (
                            <tr><td colSpan={4} className="py-12 text-center text-on-surface-variant dark:text-slate-400">{t('academic.noGradeData')}</td></tr>
                          ) : portalDetail.map((g, i) => (
                            <tr key={i} className="hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors">
                              <td className="py-4 px-3">
                                <div className="font-bold text-primary dark:text-white">{g.SBJT_NM ?? g.sbjt_nm ?? g.subjectName ?? '—'}</div>
                                <div className="text-[11px] text-outline dark:text-slate-500">{g.SBJT_CD ?? g.sbjt_cd ?? ''}</div>
                              </td>
                              <td className="py-4 px-3 text-center text-on-surface dark:text-white">{g.FAC_POINT ?? g.fac_point ?? g.credits ?? '—'}</td>
                              <td className="py-4 px-3 text-center">
                                <span className={`font-black px-3 py-1 rounded-full text-xs ${gradeColor(g.EVL_GRD_NM ?? g.evl_grd_nm ?? g.letterGrade)}`}>
                                  {g.EVL_GRD_NM ?? g.evl_grd_nm ?? g.letterGrade ?? '—'}
                                </span>
                              </td>
                              <td className="py-4 px-3 text-center font-bold text-primary dark:text-secondary-fixed">
                                {g.GPA ?? g.gpa ?? g.gradePoint ?? '—'}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {/* ── 출결 탭 ── */}
          {!loading && tab === 'attendance' && (
            <div>
              {attendance?.absenceWarnings?.length > 0 && (
                <div className="mb-4 p-4 bg-error-container dark:bg-error/20 text-error rounded-xl flex items-center gap-3">
                  <span className="material-symbols-outlined">warning</span>
                  <span className="font-bold text-sm">{t('academic.absenceWarning')}: {attendance.absenceWarnings.join(', ')}</span>
                </div>
              )}
              <div className="grid grid-cols-4 gap-4 mb-6">
                {[
                  { label: t('academic.present'), val: attendance?.totalPresent ?? '—', color: 'text-secondary dark:text-secondary-fixed' },
                  { label: t('academic.late'), val: attendance?.totalLate ?? '—', color: 'text-surface-tint dark:text-blue-400' },
                  { label: t('academic.absent'), val: attendance?.totalAbsent ?? '—', color: 'text-error' },
                  { label: t('academic.excused'), val: attendance?.totalExcused ?? '—', color: 'text-outline dark:text-slate-400' },
                ].map(s => (
                  <div key={s.label} className="card p-4 text-center">
                    <p className="text-label-md text-outline dark:text-slate-400">{s.label}</p>
                    <p className={`text-3xl font-black font-['Space_Grotesk'] ${s.color}`}>{s.val}</p>
                  </div>
                ))}
              </div>
              {portalSynced && (
                <p className="text-sm text-on-surface-variant dark:text-slate-400 text-center">
                  {t('academic.internalAttendInfo')}
                </p>
              )}
            </div>
          )}

          {/* ── 포털 시간표 탭 ── */}
          {tab === 'portal_schedule' && (
            <div>
              {/* 학년도·학기 선택 */}
              <div className="flex gap-3 mb-5">
                <select value={scheduleYear} onChange={e => { setScheduleYear(e.target.value); setPortalSchedule(null) }}
                  className="px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none">
                  {recentYears(4).map(y => <option key={y} value={y}>{t('academic.yearOption', { year: y })}</option>)}
                </select>
                <select value={scheduleSemester} onChange={e => { setScheduleSemester(e.target.value); setPortalSchedule(null) }}
                  className="px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none">
                  <option value="SU002001">{t('academic.semester1')}</option>
                  <option value="SU002002">{t('academic.semester2')}</option>
                </select>
                <button onClick={() => { setPortalSchedule(null); setScheduleLoading(true); getPortalSchedule(scheduleYear, scheduleSemester).then(r => setPortalSchedule(r.data ?? [])).catch(() => setPortalSchedule([])).finally(() => setScheduleLoading(false)) }}
                  className="px-4 py-2 bg-secondary-fixed text-primary rounded-xl text-sm font-bold flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px]">refresh</span>{t('academic.lookup')}
                </button>
              </div>

              {scheduleLoading && <div className="flex justify-center py-12"><div className="w-7 h-7 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>}

              {!scheduleLoading && portalSchedule !== null && portalSchedule.length === 0 && (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600 block mb-3">event_note</span>
                  {portalSynced ? (
                    <>
                      <p className="font-bold text-primary dark:text-white">{t('academic.scheduleEmptyOrExpired')}</p>
                      <p className="text-sm text-outline dark:text-slate-400 mt-1"><Trans i18nKey="academic.scheduleResyncHint" components={{ strong: <strong /> }} /></p>
                    </>
                  ) : (
                    <>
                      <p className="font-bold text-primary dark:text-white">{t('academic.schedulePortalLinkRequired')}</p>
                      <p className="text-sm text-outline dark:text-slate-400 mt-1">{t('academic.portalLinkFirst')}</p>
                    </>
                  )}
                </div>
              )}

              {!scheduleLoading && portalSchedule && portalSchedule.length > 0 && <ScheduleGrid data={portalSchedule} />}
            </div>
          )}

          {/* ── 포털 출결 탭 ── */}
          {tab === 'portal_attend' && (
            <div>
              {!portalAttend ? (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">fact_check</span>
                  <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('academic.portalAttendTitle')}</p>
                  <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1 mb-6">{t('academic.portalAttendDesc')}</p>
                  <button onClick={() => setShowPwModal(true)}
                    className="btn-primary mx-auto">
                    <span className="material-symbols-outlined text-[18px]">lock_open</span>{t('academic.lookupAfterPw')}
                  </button>
                </div>
              ) : (
                <div>
                  {/* 요약 */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
                    {[
                      { label: t('academic.totalPresent'), val: portalAttend.summary?.total_present ?? 0, color: 'text-secondary dark:text-secondary-fixed' },
                      { label: t('academic.absent'), val: portalAttend.summary?.total_absent ?? 0, color: 'text-error' },
                      { label: t('academic.late'), val: portalAttend.summary?.total_late ?? 0, color: 'text-surface-tint dark:text-blue-400' },
                      { label: t('academic.attendanceRate'), val: portalAttend.summary?.attendance_rate != null ? `${portalAttend.summary.attendance_rate}%` : '—', color: 'text-primary dark:text-white' },
                    ].map(s => (
                      <div key={s.label} className="card p-4 text-center">
                        <p className="text-label-md text-outline dark:text-slate-400">{s.label}</p>
                        <p className={`text-2xl font-black font-['Space_Grotesk'] ${s.color}`}>{s.val}</p>
                      </div>
                    ))}
                  </div>

                  {/* 출석률 바 */}
                  {portalAttend.summary?.attendance_rate != null && (
                    <div className="mb-6">
                      <div className="flex justify-between text-sm mb-1">
                        <span className="font-bold text-primary dark:text-white">{t('academic.overallAttendanceRate')}</span>
                        <span className="font-black text-secondary dark:text-secondary-fixed">{portalAttend.summary.attendance_rate}%</span>
                      </div>
                      <div className="w-full h-3 bg-surface-container dark:bg-slate-700 rounded-full overflow-hidden">
                        <div className="h-full bg-secondary dark:bg-secondary-fixed rounded-full transition-all duration-700"
                          style={{ width: `${portalAttend.summary.attendance_rate}%` }} />
                      </div>
                    </div>
                  )}

                  {/* 과목별 */}
                  <div className="space-y-3">
                    {(portalAttend.courses ?? []).filter(c => !c.error).map((c, i) => {
                      const total = (c.present ?? 0) + (c.absent ?? 0) + (c.late ?? 0) + (c.not_checked ?? 0)
                      const rate  = total > 0 ? Math.round((c.present / total) * 100) : null
                      return (
                        <div key={i} className="p-4 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                              <span className={`w-2 h-2 rounded-full ${c.source === 'check' ? 'bg-secondary' : 'bg-blue-400'}`} />
                              <span className="font-bold text-primary dark:text-white text-sm">{c.course_name}</span>
                              <span className="text-[10px] text-outline dark:text-slate-500 bg-surface-container dark:bg-slate-700 px-2 py-0.5 rounded">{c.source}</span>
                            </div>
                            {rate !== null && (
                              <span className={`text-sm font-black ${rate >= 75 ? 'text-secondary dark:text-secondary-fixed' : 'text-error'}`}>{rate}%</span>
                            )}
                          </div>
                          <div className="flex gap-4 text-xs text-on-surface-variant dark:text-slate-400">
                            <span>{t('academic.present')} <strong className="text-primary dark:text-white">{c.present ?? 0}</strong></span>
                            <span>{t('academic.absent')} <strong className={c.absent > 0 ? 'text-error' : 'text-primary dark:text-white'}>{c.absent ?? 0}</strong></span>
                            <span>{t('academic.late')} <strong className="text-primary dark:text-white">{c.late ?? 0}</strong></span>
                            <span>{t('academic.notChecked')} <strong className="text-outline dark:text-slate-400">{c.not_checked ?? 0}</strong></span>
                          </div>
                          {total > 0 && (
                            <div className="mt-2 w-full h-1.5 bg-surface-container dark:bg-slate-700 rounded-full overflow-hidden">
                              <div className="h-full bg-secondary dark:bg-secondary-fixed rounded-full"
                                style={{ width: `${(c.present / total) * 100}%` }} />
                            </div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                  <button onClick={() => setShowPwModal(true)}
                    className="mt-4 w-full py-2 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-400 rounded-xl text-sm hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                    <span className="material-symbols-outlined text-[16px] align-middle mr-1">refresh</span>{t('academic.lookupAgain')}
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ── 졸업요건 탭 ── */}
          {!loading && tab === 'graduation' && (
            <div>
              <div className={`mb-6 p-5 rounded-2xl border-2 ${(graduation?.isEligible ?? false) ? 'bg-secondary-container/20 dark:bg-secondary-fixed/10 border-secondary dark:border-secondary-fixed' : 'bg-surface-container dark:bg-slate-800 border-outline-variant dark:border-slate-700'}`}>
                <div className="flex items-center gap-3">
                  <span className={`material-symbols-outlined text-3xl ${(graduation?.isEligible ?? false) ? 'text-secondary dark:text-secondary-fixed' : 'text-outline dark:text-slate-400'}`}>
                    {(graduation?.isEligible ?? false) ? 'check_circle' : 'pending'}
                  </span>
                  <div>
                    <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">
                      {(graduation?.isEligible ?? false) ? t('academic.graduationEligible') : t('academic.graduationNotEligible')}
                    </p>
                    <p className="text-sm text-on-surface-variant dark:text-slate-400">
                      {t('academic.earnedCredits')} {graduation?.totalEarnedCredits ?? '—'} / {graduation?.requiredTotalCredits ?? 80} · GPA {graduation?.currentGpa?.toFixed(2) ?? '—'}
                    </p>
                  </div>
                </div>
              </div>
              <div className="space-y-3">
                {(graduation?.requirements ?? []).map((r, i) => (
                  <div key={i} className="flex items-center gap-4 p-4 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                    <span className={`material-symbols-outlined ${r.completed ? 'text-secondary dark:text-secondary-fixed' : 'text-error'}`}>
                      {r.completed ? 'check_circle' : 'cancel'}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-primary dark:text-white">{r.name}</p>
                      <p className="text-label-md text-outline dark:text-slate-400">{r.category}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-black text-primary dark:text-secondary-fixed">{r.earnedCredits}<span className="font-normal text-outline dark:text-slate-400"> / {t('academic.creditsSuffix', { count: r.requiredCredits })}</span></p>
                      <div className="w-24 h-1.5 bg-surface-container dark:bg-slate-700 rounded-full mt-1">
                        <div className={`h-full rounded-full ${r.completed ? 'bg-secondary dark:bg-secondary-fixed' : 'bg-error'}`}
                          style={{ width: `${Math.min(100, (r.earnedCredits / r.requiredCredits) * 100)}%` }} />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* ── 학습 분석 탭 ── */}
          {!loading && tab === 'analysis' && (
            <div className="space-y-6">
              <div>
                <h4 className="font-bold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">trending_up</span>
                  {t('academic.gpaTrend')}
                </h4>
                {gradeTrend.length === 0 ? (
                  <div className="h-40 flex items-center justify-center text-on-surface-variant dark:text-slate-400 text-sm bg-surface-container-low dark:bg-slate-800 rounded-2xl">
                    {t('academic.noData')}
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={gradeTrend} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                      <XAxis dataKey="semester" tick={{ fontSize: 11 }} />
                      <YAxis domain={[0, 4.5]} tick={{ fontSize: 11 }} />
                      <Tooltip formatter={(v) => [`${v.toFixed(2)}`, 'GPA']} />
                      <Bar dataKey="gpa" fill="#bff365" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>

              <div>
                <h4 className="font-bold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">calendar_today</span>
                  {t('academic.attendanceSummary')}
                </h4>
                {!attendSummary ? (
                  <div className="h-24 flex items-center justify-center text-on-surface-variant dark:text-slate-400 text-sm bg-surface-container-low dark:bg-slate-800 rounded-2xl">
                    {t('academic.noData')}
                  </div>
                ) : (
                  <div className="grid grid-cols-3 gap-4">
                    {[
                      { label: t('academic.totalPresent'), val: attendSummary.totalPresent, color: 'text-secondary dark:text-secondary-fixed' },
                      { label: t('academic.totalAbsent'), val: attendSummary.totalAbsent, color: 'text-error' },
                      { label: t('academic.totalLate'), val: attendSummary.totalLate, color: 'text-surface-tint dark:text-blue-400' },
                    ].map(s => (
                      <div key={s.label} className="card p-4 text-center">
                        <p className="text-label-md text-outline dark:text-slate-400">{s.label}</p>
                        <p className={`text-2xl font-black ${s.color}`}>{s.val ?? '—'}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ── 강의 리뷰 탭 ── */}
          {!loading && tab === 'reviews' && (
            <div>
              <div className="flex justify-between items-center mb-4">
                <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('academic.reviewCount', { count: reviews.length })}</p>
                <button onClick={() => setShowReviewForm(true)}
                  className="flex items-center gap-1.5 px-4 py-2 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  <span className="material-symbols-outlined text-[16px]">add</span>{t('academic.writeReview')}
                </button>
              </div>
              {reviews.length === 0 ? (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">rate_review</span>
                  <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('academic.noReviews')}</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {reviews.map(r => (
                    <div key={r.id} className="card p-5 group">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <span className="font-bold text-primary dark:text-white">{r.subjectName}</span>
                          <span className="ml-2 text-[11px] text-outline dark:text-slate-500">{r.anonymous ? t('academic.anonymous') : r.studentName}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="flex">
                            {[1,2,3,4,5].map(n => (
                              <span key={n} className={`material-symbols-outlined text-[16px] ${n <= r.rating ? 'text-yellow-400' : 'text-outline dark:text-slate-600'}`}>star</span>
                            ))}
                          </div>
                          <button onClick={async () => { await deleteReview(r.id); loadReviews() }}
                            className="opacity-0 group-hover:opacity-100 p-1 rounded-lg bg-error-container dark:bg-error/20 text-error transition-all">
                            <span className="material-symbols-outlined text-[14px]">delete</span>
                          </button>
                        </div>
                      </div>
                      <p className="text-sm text-on-surface dark:text-slate-200">{r.content}</p>
                      <p className="text-[11px] text-outline dark:text-slate-500 mt-2">{r.createdAt?.slice(0,10)}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── 수상내역 탭 ── */}
          {tab === 'planner' && <AiStudyPanel />}

          {!loading && tab === 'awards' && (
            <div>
              <div className="flex justify-between items-center mb-4">
                <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('academic.awardCount', { count: awards.length })}</p>
                <button onClick={() => setShowAwardForm(true)}
                  className="flex items-center gap-1.5 px-4 py-2 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  <span className="material-symbols-outlined text-[16px]">add</span>{t('academic.addAward')}
                </button>
              </div>
              {awards.length === 0 ? (
                <div className="text-center py-16">
                  <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">workspace_premium</span>
                  <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('academic.noAwards')}</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {awards.map(a => (
                    <div key={a.id} className="card p-5 group relative">
                      <div className="flex items-start justify-between mb-3">
                        <span className={`text-[11px] font-black px-3 py-1 rounded-full ${AWARD_LEVEL_COLOR[a.level]}`}>{t(AWARD_LEVEL_I18N[a.level])}</span>
                        <button onClick={async () => { await deleteAward(a.id); loadAwards() }}
                          className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg bg-error-container dark:bg-error/20 text-error transition-all">
                          <span className="material-symbols-outlined text-[15px]">delete</span>
                        </button>
                      </div>
                      <h4 className="font-bold text-primary dark:text-white mb-1">{a.title}</h4>
                      <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-1">{a.organization}</p>
                      <p className="text-label-md text-outline dark:text-slate-500">{a.awardDate}</p>
                      {a.description && <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-2 line-clamp-2">{a.description}</p>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* 포털 출결 비밀번호 모달 */}
      {showPwModal && (
        <PasswordModal
          title={t('academic.portalAttendTitle')}
          loading={attendLoading}
          onConfirm={handlePortalAttend}
          onClose={() => setShowPwModal(false)}
        />
      )}

      {/* 강의 리뷰 작성 모달 */}
      {showReviewForm && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setShowReviewForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">rate_review</span>{t('academic.writeReviewTitle')}
              </h3>
              <button onClick={() => setShowReviewForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form className="p-6 space-y-4" onSubmit={async e => {
              e.preventDefault()
              try { await createReview(reviewForm); setShowReviewForm(false); loadReviews() }
              catch { alert(t('academic.saveError')) }
            }}>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('academic.colSubject')}</label>
                <input required value={reviewForm.subjectName} onChange={e => setReviewForm(p => ({...p, subjectName: e.target.value}))}
                  placeholder={t('academic.reviewSubjectPlaceholder')}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">{t('academic.rating')}</label>
                <div className="flex gap-1">
                  {[1,2,3,4,5].map(n => (
                    <button key={n} type="button" onClick={() => setReviewForm(p => ({...p, rating: n}))}
                      className={`material-symbols-outlined text-[28px] transition-colors ${n <= reviewForm.rating ? 'text-yellow-400' : 'text-outline dark:text-slate-600'}`}>
                      star
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('academic.content')}</label>
                <textarea required value={reviewForm.content} onChange={e => setReviewForm(p => ({...p, content: e.target.value}))} rows={4}
                  placeholder={t('academic.reviewContentPlaceholder')}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" checked={reviewForm.anonymous} onChange={e => setReviewForm(p => ({...p, anonymous: e.target.checked}))}
                  className="rounded" />
                <span className="text-sm text-on-surface-variant dark:text-slate-300">{t('academic.writeAnonymously')}</span>
              </label>
              <div className="flex gap-3">
                <button type="button" onClick={() => setShowReviewForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" className="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold">{t('academic.submitReview')}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 수상내역 작성 모달 */}
      {showAwardForm && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => e.target === e.currentTarget && setShowAwardForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>
                {t('academic.addAward')}
              </h3>
              <button onClick={() => setShowAwardForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form className="p-6 space-y-4" onSubmit={async e => {
              e.preventDefault()
              try { await createAward(awardForm); setShowAwardForm(false); loadAwards() }
              catch { alert(t('academic.saveError')) }
            }}>
              {[
                { key:'title',        label: t('academic.awardTitle'), placeholder: t('academic.awardTitlePlaceholder') },
                { key:'organization', label: t('academic.awardOrg'),   placeholder: t('academic.awardOrgPlaceholder') },
              ].map(f => (
                <div key={f.key}>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{f.label}</label>
                  <input required value={awardForm[f.key]} onChange={e => setAwardForm(p => ({...p, [f.key]: e.target.value}))}
                    placeholder={f.placeholder}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
              ))}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('academic.awardLevel')}</label>
                  <select value={awardForm.level} onChange={e => setAwardForm(p => ({...p, level: e.target.value}))}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30">
                    {AWARD_LEVELS.map(l => <option key={l} value={l}>{t(AWARD_LEVEL_I18N[l])}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('academic.awardDate')}</label>
                  <input required type="date" value={awardForm.awardDate} onChange={e => setAwardForm(p => ({...p, awardDate: e.target.value}))}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('academic.awardDesc')}</label>
                <textarea value={awardForm.description} onChange={e => setAwardForm(p => ({...p, description: e.target.value}))} rows={2}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setShowAwardForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold">{t('common.save')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
