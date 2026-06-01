import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import useAuthStore from '../store/authStore'
import Layout from '../components/layout/Layout'
import { getMyGrades } from '../api/grade'
import { getMyAttendance } from '../api/attendance'
import { getTodaySchedule } from '../api/schedule'
import { getRecentNotices } from '../api/notice'
import { getTodayMeal } from '../api/meal'
import { getMyWarnings } from '../api/planner'
import { getProfile } from '../api/profile'

const today = new Date()
const weekday = ['일', '월', '화', '수', '목', '금', '토'][today.getDay()]
const dateStr = `${today.getMonth() + 1}월 ${today.getDate()}일 ${weekday}요일`

const calcStatus = (startTime, endTime) => {
  const now = new Date()
  const d = now.toISOString().split('T')[0]
  const start = new Date(`${d}T${startTime}:00`)
  const end   = new Date(`${d}T${endTime}:00`)
  if (now < start) return { label: '대기',    cls: 'bg-surface-container-low dark:bg-slate-900 text-outline dark:text-slate-500' }
  if (now <= end)  return { label: '수업 중', cls: 'bg-secondary-container dark:bg-secondary-container text-primary dark:text-[#131f00] animate-pulse' }
  return               { label: '종료',    cls: 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400' }
}

const FACILITIES = [
  { title: '도서관', sub: '학술 자료 검색 및 좌석 현황', path: '/facilities', icon: 'local_library' },
  { title: '스터디룸', sub: '그룹 스터디 공간 안내', path: '/facilities', icon: 'groups' },
  { title: '통학버스', sub: '포털 연동 실시간 운행 정보', path: '/facilities', icon: 'directions_bus' },
]

export default function Dashboard() {
  const user = useAuthStore(s => s.user)
  const navigate = useNavigate()
  const { t } = useTranslation()

  const [profile, setProfileData] = useState(null)
  const [gradeSummary, setGradeSummary] = useState(null)
  const [attendanceSummary, setAttendanceSummary] = useState(null)
  const [todaySchedule, setTodaySchedule] = useState(null)
  const [notices, setNotices] = useState(null)
  const [meal, setMeal]       = useState(null)
  const [warning, setWarning] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      getProfile().catch(() => null),
      getMyGrades().catch(() => null),
      getMyAttendance().catch(() => null),
      getTodaySchedule().catch(() => null),
      getRecentNotices(3).catch(() => null),
      getTodayMeal().catch(() => null),
      getMyWarnings().catch(() => null),
    ]).then(([profileRes, gradeRes, attendanceRes, scheduleRes, noticeRes, mealRes, warningRes]) => {
      if (profileRes?.data)    setProfileData(profileRes.data)
      if (gradeRes?.data)     setGradeSummary(gradeRes.data)
      if (attendanceRes?.data) setAttendanceSummary(attendanceRes.data)
      if (scheduleRes?.data)  setTodaySchedule(scheduleRes.data)
      if (noticeRes?.data)    setNotices(noticeRes.data)
      if (mealRes?.data)      setMeal(mealRes.data)
      if (warningRes?.data)   setWarning(warningRes.data)
    }).finally(() => setLoading(false))
  }, [])

  const gpaDisplay = loading ? '…' : gradeSummary?.gpa != null ? gradeSummary.gpa.toFixed(2) : '—'
  const creditsDisplay = loading ? '…' : gradeSummary?.totalCredits ?? '—'
  const absenceWarnings = attendanceSummary?.absenceWarnings ?? []

  const attendanceRate = attendanceSummary
    ? (() => {
        const total = attendanceSummary.totalPresent + attendanceSummary.totalLate
          + attendanceSummary.totalAbsent + attendanceSummary.totalExcused
        return total > 0 ? Math.round((attendanceSummary.totalPresent / total) * 100) : 100
      })()
    : null

  return (
    <Layout>
      {/* ── 조기경보 배너 (위험 시만 표시) ── */}
      {warning && warning.level !== 'SAFE' && (
        <div className={`mb-4 px-4 py-3 rounded-xl flex items-start gap-3 border
          ${warning.level === 'DANGER'
            ? 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
            : 'bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800'}`}>
          <span className={`material-symbols-outlined text-[22px] mt-0.5 ${warning.level === 'DANGER' ? 'text-red-500' : 'text-yellow-500'}`}>warning</span>
          <div className="flex-1">
            <p className={`text-sm font-bold ${warning.level === 'DANGER' ? 'text-red-700 dark:text-red-400' : 'text-yellow-700 dark:text-yellow-400'}`}>
              {warning.level === 'DANGER' ? '🚨 학업 위험 경고' : '⚠️ 학업 주의'}
            </p>
            <p className={`text-xs mt-0.5 ${warning.level === 'DANGER' ? 'text-red-600 dark:text-red-300' : 'text-yellow-600 dark:text-yellow-300'}`}>
              {warning.warnings?.join(' · ')}
            </p>
          </div>
        </div>
      )}

      {/* ── Mobile hero card ── */}
      <section className="lg:hidden bg-primary dark:bg-primary-container rounded-xl p-6 text-white mb-6 relative overflow-hidden shadow-xl">
        <div className="absolute top-0 right-0 opacity-10">
          <span className="material-symbols-outlined text-[120px]">school</span>
        </div>
        <span className="bg-secondary-container text-on-secondary-container text-[10px] font-bold px-2 py-0.5 rounded">{profile?.department ?? '컴퓨터정보과'}</span>
        <h1 className="font-['Space_Grotesk'] text-xl font-black mt-2">{t('dashboard.greeting', { name: user?.name ?? '학생' })}</h1>
        <div className="grid grid-cols-2 gap-3 mt-4">
          <div className="bg-white/10 backdrop-blur-sm p-3 rounded-lg border border-white/10">
            <p className="text-[10px] opacity-70 uppercase tracking-tight">{t('dashboard.gpa')}</p>
            <p className="text-xl font-black font-['Space_Grotesk']">{gpaDisplay} / 4.5</p>
          </div>
          <div className="bg-white/10 backdrop-blur-sm p-3 rounded-lg border border-white/10">
            <p className="text-[10px] opacity-70 uppercase tracking-tight">{t('dashboard.credits')}</p>
            <p className="text-xl font-black font-['Space_Grotesk']">{creditsDisplay} 학점</p>
          </div>
        </div>
        {absenceWarnings.length > 0 && (
          <div className="mt-3 bg-red-500/20 border border-red-400/30 rounded-lg px-3 py-2 text-xs text-red-200 flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[14px]">warning</span>
            결석 경고: {absenceWarnings.join(', ')}
          </div>
        )}
      </section>

      {/* ── Mobile schedule ── */}
      <section className="lg:hidden space-y-3 mb-6">
        <h2 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary-fixed text-[20px]">calendar_today</span>오늘의 강의
        </h2>
        {todaySchedule === null
          ? <p className="text-sm text-outline dark:text-slate-500 text-center py-4">불러오는 중…</p>
          : todaySchedule.length === 0
            ? <p className="text-sm text-outline dark:text-slate-500 text-center py-4">오늘 강의가 없습니다. 시간표를 등록해보세요.</p>
            : todaySchedule.map((c) => {
                const { label, cls } = calcStatus(c.startTime, c.endTime)
                return (
                  <div key={c.id} className="bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-100 dark:border-slate-800 flex gap-4 items-center shadow-sm">
                    <div className="flex flex-col items-center justify-center w-16 h-16 bg-surface-container-low dark:bg-slate-800 rounded-lg border-l-4 border-secondary-fixed shrink-0">
                      <span className="text-xs font-bold text-on-surface-variant dark:text-slate-400">{c.startTime}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-primary dark:text-white truncate">{c.subjectName}</p>
                      <p className="text-xs text-on-surface-variant dark:text-slate-400 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">location_on</span>{c.room || '강의실 미지정'}
                      </p>
                    </div>
                    <span className={`px-2 py-1 rounded-full text-[10px] font-bold shrink-0 ${cls}`}>{label}</span>
                  </div>
                )
              })
        }
      </section>

      {/* ── Mobile quick actions ── */}
      <section className="lg:hidden grid grid-cols-4 gap-4 text-center mb-6">
        {[
          { icon: 'receipt_long', label: '성적조회', path: '/academic' },
          { icon: 'fact_check',   label: '수강신청', path: '/academic' },
          { icon: 'work',         label: '진로로드맵', path: '/career' },
          { icon: 'description',  label: '포트폴리오', path: '/technical' },
        ].map(a => (
          <button key={a.label} onClick={() => navigate(a.path)} className="flex flex-col items-center gap-2">
            <div className="w-12 h-12 rounded-xl bg-white dark:bg-slate-800 shadow-sm border border-slate-100 dark:border-slate-700 flex items-center justify-center text-primary dark:text-secondary-fixed active:scale-95 transition-transform">
              <span className="material-symbols-outlined">{a.icon}</span>
            </div>
            <span className="text-[10px] font-medium text-on-surface-variant dark:text-slate-400">{a.label}</span>
          </button>
        ))}
      </section>

      {/* ── Desktop layout ── */}
      <div className="hidden lg:grid grid-cols-12 gap-card_gap">

        {/* Left: Profile + Notice */}
        <section className="col-span-4 flex flex-col gap-card_gap">
          {/* Profile card */}
          <div className="bg-white dark:bg-slate-900 p-6 rounded-xl shadow-sm border border-slate-100 dark:border-slate-800 flex flex-col gap-4 relative overflow-hidden group hover:shadow-md transition-shadow">
            <div className="absolute top-0 right-0 w-32 h-32 bg-secondary-fixed/10 rounded-full -mr-16 -mt-16 group-hover:scale-110 transition-transform" />
            <div className="flex items-center gap-4 relative">
              <div className="w-16 h-16 rounded-xl bg-primary-container dark:bg-slate-800 flex items-center justify-center text-secondary-fixed shrink-0 border border-slate-200 dark:border-slate-700">
                <span className="material-symbols-outlined text-3xl icon-fill">person</span>
              </div>
              <div>
                <h2 className="font-['Space_Grotesk'] text-2xl font-semibold text-primary dark:text-white">{user?.name ?? '학생'}</h2>
                <p className="text-sm text-outline dark:text-slate-400">{profile?.department ?? '컴퓨터정보과'} · {profile?.grade ?? '—'}학년 {profile?.semester ?? '—'}학기</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg border border-slate-50 dark:border-slate-700">
                <p className="text-label-md text-outline dark:text-slate-500">평균 학점</p>
                <p className="text-title-lg font-semibold text-primary dark:text-secondary-fixed">{gpaDisplay} / 4.5</p>
              </div>
              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg border border-slate-50 dark:border-slate-700">
                <p className="text-label-md text-outline dark:text-slate-500">이수 학점</p>
                <p className="text-title-lg font-semibold text-primary dark:text-secondary-fixed">{creditsDisplay} 학점</p>
              </div>
            </div>
            <button onClick={() => navigate('/academic')} className="w-full py-3 bg-primary dark:bg-primary-container text-white rounded-lg text-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2">
              프로필 상세보기 <span className="material-symbols-outlined text-sm">arrow_forward</span>
            </button>
          </div>

          {/* Notice card */}
          <div className="bg-primary dark:bg-[#0f172a] p-6 rounded-xl shadow-lg text-white flex flex-col gap-4 relative overflow-hidden border dark:border-slate-800">
            <div className="absolute bottom-0 right-0 opacity-5">
              <span className="material-symbols-outlined text-[120px]">campaign</span>
            </div>
            <div className="flex items-center justify-between">
              <h3 className="font-['Space_Grotesk'] text-lg font-medium flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary-fixed">info</span>중요 공지사항
              </h3>
              {notices?.some(n => n.important) && (
                <span className="bg-secondary-fixed text-primary text-[10px] px-2 py-1 rounded font-bold">New</span>
              )}
            </div>
            <ul className="flex flex-col gap-3 relative z-10">
              {notices === null
                ? <li className="text-sm text-slate-300 text-center py-2">불러오는 중…</li>
                : notices.length === 0
                  ? <li className="text-sm text-slate-300 text-center py-2">등록된 공지사항이 없습니다.</li>
                  : notices.map((n, i) => (
                    <li key={n.id} className={`pb-2 ${i < notices.length - 1 ? 'border-b border-white/10 dark:border-slate-800' : ''}`}>
                      <p className={`text-sm font-medium ${n.important ? 'text-secondary-fixed' : 'text-slate-100 dark:text-slate-200'}`}>{n.title}</p>
                      {n.summary && <p className="text-[11px] text-slate-300 dark:text-slate-400">{n.summary}</p>}
                    </li>
                  ))
              }
            </ul>
          </div>
        </section>

        {/* Right: Schedule */}
        <section className="col-span-8 flex flex-col gap-card_gap">
          <div className="bg-white dark:bg-slate-900 p-6 rounded-xl shadow-sm border border-slate-100 dark:border-slate-800 flex-1">
            <div className="flex items-center justify-between mb-6">
              <h3 className="font-['Space_Grotesk'] text-2xl font-semibold text-primary dark:text-white flex items-center gap-3">
                <span className="material-symbols-outlined text-primary dark:text-secondary-fixed p-2 bg-secondary-container/30 dark:bg-secondary-fixed/10 rounded-lg">calendar_today</span>
                오늘의 강의 시간표
              </h3>
              <span className="text-outline dark:text-slate-500 text-label-md flex items-center gap-1.5">
                <span className="w-2 h-2 bg-secondary-fixed rounded-full" />{dateStr}
              </span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-slate-100 dark:border-slate-800">
                    {['시간', '강의명', '강의실', '교수', '상태'].map((h, i) => (
                      <th key={h} className={`py-4 text-label-md text-outline dark:text-slate-500 uppercase tracking-wider ${i === 4 ? 'text-right' : ''}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                  {todaySchedule === null
                    ? <tr><td colSpan={5} className="py-8 text-center text-outline dark:text-slate-500 text-sm">불러오는 중…</td></tr>
                    : todaySchedule.length === 0
                      ? <tr><td colSpan={5} className="py-8 text-center text-outline dark:text-slate-500 text-sm">오늘 강의가 없습니다. 시간표를 등록해보세요.</td></tr>
                      : todaySchedule.map((c) => {
                          const { label, cls } = calcStatus(c.startTime, c.endTime)
                          return (
                            <tr key={c.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors">
                              <td className="py-5 text-sm text-primary dark:text-slate-300">{c.startTime} - {c.endTime}</td>
                              <td className="py-5">
                                <p className="font-bold text-primary dark:text-slate-100">{c.subjectName}</p>
                                {c.subjectCode && <p className="text-label-md text-outline dark:text-slate-500">{c.subjectCode}</p>}
                              </td>
                              <td className="py-5 text-sm text-outline dark:text-slate-400">{c.room || '—'}</td>
                              <td className="py-5 text-sm text-outline dark:text-slate-400">{c.professor || '—'}</td>
                              <td className="py-5 text-right">
                                <span className={`px-3 py-1 rounded-full text-[11px] font-bold ${cls}`}>{label}</span>
                              </td>
                            </tr>
                          )
                        })
                  }
                </tbody>
              </table>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-4">
              <div className="bg-surface-container-low dark:bg-slate-800 rounded-xl p-4 flex items-center gap-4 border-l-4 border-secondary-fixed">
                <div className="p-3 bg-white dark:bg-slate-900 rounded-lg shadow-sm border border-slate-100 dark:border-slate-700">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">how_to_reg</span>
                </div>
                <div>
                  <p className="text-label-md text-outline dark:text-slate-500">출석률</p>
                  <p className="font-bold text-primary dark:text-white">
                    {attendanceRate != null ? `${attendanceRate}%` : '—'}
                    {attendanceSummary && (
                      <span className="text-label-md text-outline dark:text-slate-400 ml-2">
                        결석 {attendanceSummary.totalAbsent}회
                      </span>
                    )}
                  </p>
                </div>
              </div>
              <div className="bg-surface-container-low dark:bg-slate-800 rounded-xl p-4 flex items-center gap-4 border-l-4 border-primary dark:border-primary-container">
                <div className="p-3 bg-white dark:bg-slate-900 rounded-lg shadow-sm border border-slate-100 dark:border-slate-700">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">workspace_premium</span>
                </div>
                <div>
                  <p className="text-label-md text-outline dark:text-slate-500">취득 학점</p>
                  <p className="font-bold text-primary dark:text-white">{creditsDisplay} 학점</p>
                </div>
              </div>
            </div>
            {absenceWarnings.length > 0 && (
              <div className="mt-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-4 flex items-start gap-3">
                <span className="material-symbols-outlined text-red-500 dark:text-red-400 text-[20px] mt-0.5">warning</span>
                <div>
                  <p className="text-sm font-bold text-red-700 dark:text-red-400">결석 경고 과목</p>
                  <p className="text-sm text-red-600 dark:text-red-300 mt-0.5">{absenceWarnings.join(' · ')}</p>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Bottom: Facility cards */}
        <section className="col-span-12">
          <div className="grid grid-cols-3 gap-card_gap">
            {FACILITIES.map((f, i) => (
              <button key={i} onClick={() => navigate(f.path)}
                className="h-36 rounded-xl bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 shadow-sm flex flex-col items-center justify-center gap-3 hover:shadow-md hover:border-secondary-fixed/40 dark:hover:border-secondary-fixed/40 active:scale-[0.98] transition-all group">
                <div className="w-12 h-12 rounded-xl bg-primary-container/30 dark:bg-secondary-fixed/10 flex items-center justify-center group-hover:bg-secondary-fixed/20 transition-colors">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed text-[28px]">{f.icon}</span>
                </div>
                <div className="text-center">
                  <p className="font-bold text-primary dark:text-white text-sm">{f.title}</p>
                  <p className="text-[11px] text-outline dark:text-slate-400 mt-0.5">{f.sub}</p>
                </div>
              </button>
            ))}
          </div>
        </section>
      </div>
    </Layout>
  )
}
