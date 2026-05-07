import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getMyGrades } from '../api/grade'
import { getMyAttendance } from '../api/attendance'
import { checkGraduation } from '../api/graduation'
import { getMyAwards, createAward, deleteAward } from '../api/award'

const AWARD_LEVELS = ['GOLD','SILVER','BRONZE','ENCOURAGEMENT','PARTICIPATION']
const AWARD_LEVEL_LABEL = { GOLD:'금상', SILVER:'은상', BRONZE:'동상', ENCOURAGEMENT:'장려상', PARTICIPATION:'입상' }
const AWARD_LEVEL_COLOR = {
  GOLD:          'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400',
  SILVER:        'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300',
  BRONZE:        'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-400',
  ENCOURAGEMENT: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400',
  PARTICIPATION: 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400',
}

const WEEKS = ['1주','2주','3주','4주','5주','6주','7주','8주','9주','10주','11주','12주']
const MOCK_ATTENDANCE = ['P','P','P','P','P','L','P','P','P','P','P',null]

function AttendanceBar({ status }) {
  if (!status) return <div className="w-full h-8 bg-surface-container dark:bg-slate-800 rounded-lg border border-dashed border-outline dark:border-slate-700" />
  const color = status === 'P' ? 'bg-secondary dark:bg-secondary-fixed/80' : status === 'L' ? 'bg-surface-tint' : 'bg-error'
  return <div className={`w-full h-8 ${color} rounded-lg`} />
}

export default function Academic() {
  const { t } = useTranslation()
  const TABS = [
    { key: 'grade',      icon: 'grade',             label: t('academic.tab_grade')      },
    { key: 'attendance', icon: 'calendar_today',     label: t('academic.tab_attendance') },
    { key: 'graduation', icon: 'school',             label: t('academic.tab_graduation') },
    { key: 'awards',     icon: 'workspace_premium',  label: t('academic.tab_awards')     },
  ]

  const [tab, setTab] = useState('grade')
  const [grades, setGrades] = useState(null)
  const [attendance, setAttendance] = useState(null)
  const [graduation, setGraduation] = useState(null)
  const [awards, setAwards] = useState([])
  const [loading, setLoading] = useState(false)
  const [showAwardForm, setShowAwardForm] = useState(false)
  const [awardForm, setAwardForm] = useState({ title:'', organization:'', level:'GOLD', awardDate:'', description:'' })

  const loadAwards = () => getMyAwards().then(r => setAwards(r.data ?? [])).catch(() => setAwards([]))

  useEffect(() => {
    setLoading(true)
    if (tab === 'grade') {
      getMyGrades().then(r => setGrades(r.data)).catch(() => setGrades(null)).finally(() => setLoading(false))
    } else if (tab === 'attendance') {
      getMyAttendance().then(r => setAttendance(r.data)).catch(() => setAttendance(null)).finally(() => setLoading(false))
    } else if (tab === 'awards') {
      loadAwards().finally(() => setLoading(false))
    } else {
      checkGraduation().then(r => setGraduation(r.data)).catch(() => setGraduation(null)).finally(() => setLoading(false))
    }
  }, [tab])

  const gradeColor = (g) => {
    if (!g) return ''
    if (g.startsWith('A')) return 'bg-secondary-container dark:bg-secondary-fixed text-on-secondary-container dark:text-[#131f00]'
    if (g.startsWith('B')) return 'bg-surface-container-highest dark:bg-slate-700 text-on-surface dark:text-white'
    return 'bg-error-container dark:bg-error/20 text-error'
  }

  return (
    <Layout title="학사 정보 서비스">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">나의 학업 대시보드</h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">성적, 출결 현황 및 졸업요건을 확인하세요.</p>
        </div>
        <div className="flex gap-2">
          <button className="btn-primary text-sm">
            <span className="material-symbols-outlined text-[18px]">download</span>증명서 발급
          </button>
          <button className="btn-secondary text-sm">
            <span className="material-symbols-outlined text-[18px]">print</span>성적표
          </button>
        </div>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">전체 평점</span>
            <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">military_tech</span>
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-black text-primary dark:text-white font-['Space_Grotesk']">
              {grades?.gpa?.toFixed(2) ?? '4.2'}
            </span>
            <span className="text-on-surface-variant dark:text-slate-400">/ 4.5</span>
          </div>
          <div className="mt-3 flex items-center gap-1 text-secondary dark:text-secondary-fixed text-label-md font-bold">
            <span className="material-symbols-outlined text-[14px]">trending_up</span>지난 학기 대비 +0.3
          </div>
        </div>

        <div className="card p-5">
          <div className="flex justify-between items-center mb-3">
            <span className="text-on-surface-variant dark:text-slate-400 text-label-md">출석률</span>
            <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">task_alt</span>
          </div>
          <div className="text-3xl font-black text-secondary dark:text-white font-['Space_Grotesk']">
            {attendance ? `${Math.round((attendance.totalPresent / Math.max(attendance.totalPresent + attendance.totalAbsent + attendance.totalLate, 1)) * 100)}%` : '98%'}
          </div>
          <div className="mt-3 w-full bg-surface-container dark:bg-slate-700 rounded-full h-2">
            <div className="bg-secondary dark:bg-secondary-fixed h-2 rounded-full" style={{ width: '98%' }} />
          </div>
        </div>

        <div className="card p-5 col-span-2 lg:col-span-2 bg-primary-container dark:bg-[#1a2b4a] border-0 text-white relative overflow-hidden">
          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-2">
              <span className="bg-secondary-container text-on-secondary-container dark:bg-secondary-fixed dark:text-[#131f00] text-[10px] font-black px-2 py-0.5 rounded uppercase">Important</span>
              <span className="text-on-primary-container dark:text-slate-300 text-label-md">강의 평가 기간</span>
            </div>
            <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-white">
              미완료된 강의 평가가 <span className="text-secondary-fixed">2건</span> 있습니다.
            </h3>
            <button className="mt-3 bg-secondary-fixed text-on-secondary-fixed px-5 py-2 rounded-full font-bold text-label-md hover:bg-white transition-colors">
              지금 참여하기
            </button>
          </div>
          <span className="material-symbols-outlined absolute -bottom-6 -right-6 text-[140px] opacity-10">rate_review</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 overflow-hidden">
        <div className="flex border-b border-slate-100 dark:border-slate-800 px-2 lg:px-6 overflow-x-auto">
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`flex items-center gap-2 px-4 lg:px-6 py-4 text-sm font-bold border-b-2 shrink-0 transition-colors ${
                tab === t.key
                  ? 'border-primary dark:border-secondary-fixed text-primary dark:text-secondary-fixed'
                  : 'border-transparent text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
              }`}
            >
              <span className="material-symbols-outlined text-[18px]">{t.icon}</span>
              <span className="hidden sm:inline">{t.label}</span>
            </button>
          ))}
        </div>

        <div className="p-4 lg:p-6">
          {loading && (
            <div className="flex justify-center py-12">
              <div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
            </div>
          )}

          {/* Grade tab */}
          {!loading && tab === 'grade' && (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-slate-100 dark:border-slate-800">
                    {['교과목명','이수구분','학점','중간','기말','성적','등급'].map((h, i) => (
                      <th key={h} className={`py-3 px-3 text-label-md text-outline dark:text-slate-500 uppercase tracking-wider ${i >= 2 ? 'text-center' : ''}`}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                  {(grades?.grades ?? [
                    { subjectName: '알고리즘 및 실습', subjectCode: 'CSE3021', credits: 3, letterGrade: 'A+', gradePoint: 4.5 },
                    { subjectName: '데이터베이스 시스템', subjectCode: 'CSE3025', credits: 3, letterGrade: 'A+', gradePoint: 4.5 },
                    { subjectName: '인공지능 개론', subjectCode: 'CSE4011', credits: 3, letterGrade: 'B+', gradePoint: 3.5 },
                  ]).map((g, i) => (
                    <tr key={i} className="hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors">
                      <td className="py-4 px-3">
                        <div className="font-bold text-primary dark:text-white">{g.subjectName}</div>
                        <div className="text-[11px] text-outline dark:text-slate-500">{g.subjectCode}</div>
                      </td>
                      <td className="py-4 px-3">
                        <span className="bg-surface-container dark:bg-slate-700 text-on-surface-variant dark:text-slate-300 px-2 py-1 rounded text-[11px]">전공필수</span>
                      </td>
                      <td className="py-4 px-3 text-center font-medium text-on-surface dark:text-white">{g.credits}</td>
                      <td className="py-4 px-3 text-center text-outline dark:text-slate-400">-</td>
                      <td className="py-4 px-3 text-center text-outline dark:text-slate-400">-</td>
                      <td className="py-4 px-3 text-center font-bold text-primary dark:text-secondary-fixed">{Math.round(g.gradePoint * 20)}</td>
                      <td className="py-4 px-3 text-center">
                        <span className={`font-black px-3 py-1 rounded-full text-xs ${gradeColor(g.letterGrade)}`}>{g.letterGrade}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Attendance tab */}
          {!loading && tab === 'attendance' && (
            <div>
              {attendance?.absenceWarnings?.length > 0 && (
                <div className="mb-4 p-4 bg-error-container dark:bg-error/20 text-error rounded-xl flex items-center gap-3">
                  <span className="material-symbols-outlined">warning</span>
                  <span className="font-bold text-sm">결석 3회 이상 경고: {attendance.absenceWarnings.join(', ')}</span>
                </div>
              )}
              <div className="grid grid-cols-4 gap-4 mb-6">
                {[
                  { label: '출석', val: attendance?.totalPresent ?? 42, color: 'text-secondary dark:text-secondary-fixed' },
                  { label: '지각', val: attendance?.totalLate ?? 1, color: 'text-surface-tint dark:text-blue-400' },
                  { label: '결석', val: attendance?.totalAbsent ?? 0, color: 'text-error' },
                  { label: '공결', val: attendance?.totalExcused ?? 0, color: 'text-outline dark:text-slate-400' },
                ].map(s => (
                  <div key={s.label} className="card p-4 text-center">
                    <p className="text-label-md text-outline dark:text-slate-400">{s.label}</p>
                    <p className={`text-3xl font-black font-['Space_Grotesk'] ${s.color}`}>{s.val}</p>
                  </div>
                ))}
              </div>
              <h4 className="font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">verified_user</span>주차별 출결 요약
              </h4>
              <div className="flex gap-2 overflow-x-auto pb-2">
                {WEEKS.map((w, i) => (
                  <div key={w} className="flex-1 flex flex-col items-center gap-2 min-w-[40px]">
                    <span className="text-[10px] font-bold text-on-surface-variant dark:text-slate-500">{w}</span>
                    <AttendanceBar status={MOCK_ATTENDANCE[i]} />
                  </div>
                ))}
              </div>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-3 italic text-center">알고리즘 및 실습 과목 출결 현황 (현재 11주차)</p>
            </div>
          )}

          {/* Graduation tab */}
          {!loading && tab === 'graduation' && (
            <div>
              <div className={`mb-6 p-5 rounded-2xl border-2 ${(graduation?.isEligible ?? false) ? 'bg-secondary-container/20 dark:bg-secondary-fixed/10 border-secondary dark:border-secondary-fixed' : 'bg-surface-container dark:bg-slate-800 border-outline-variant dark:border-slate-700'}`}>
                <div className="flex items-center gap-3">
                  <span className={`material-symbols-outlined text-3xl ${(graduation?.isEligible ?? false) ? 'text-secondary dark:text-secondary-fixed' : 'text-outline dark:text-slate-400'}`}>
                    {(graduation?.isEligible ?? false) ? 'check_circle' : 'pending'}
                  </span>
                  <div>
                    <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">
                      {(graduation?.isEligible ?? false) ? '졸업 요건 충족' : '졸업 요건 미충족'}
                    </p>
                    <p className="text-sm text-on-surface-variant dark:text-slate-400">
                      이수 학점 {graduation?.totalEarnedCredits ?? 98} / {graduation?.requiredTotalCredits ?? 80} · GPA {(graduation?.currentGpa ?? 4.2).toFixed(2)}
                    </p>
                  </div>
                </div>
              </div>
              <div className="space-y-3">
                {(graduation?.requirements ?? [
                  { category: '전공필수', name: '핵심 전공', earnedCredits: 24, requiredCredits: 24, completed: true },
                  { category: '전공선택', name: '전공선택', earnedCredits: 18, requiredCredits: 21, completed: false },
                  { category: '교양필수', name: '교양필수', earnedCredits: 12, requiredCredits: 12, completed: true },
                ]).map((r, i) => (
                  <div key={i} className="flex items-center gap-4 p-4 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                    <span className={`material-symbols-outlined ${r.completed ? 'text-secondary dark:text-secondary-fixed' : 'text-error'}`}>
                      {r.completed ? 'check_circle' : 'cancel'}
                    </span>
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-primary dark:text-white">{r.name}</p>
                      <p className="text-label-md text-outline dark:text-slate-400">{r.category}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-black text-primary dark:text-secondary-fixed">{r.earnedCredits}<span className="font-normal text-outline dark:text-slate-400"> / {r.requiredCredits}학점</span></p>
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

          {/* Awards tab */}
          {!loading && tab === 'awards' && (
            <div>
              <div className="flex justify-between items-center mb-4">
                <p className="text-sm text-on-surface-variant dark:text-slate-400">{awards.length}개의 수상 내역</p>
                <button
                  onClick={() => setShowAwardForm(true)}
                  className="flex items-center gap-1.5 px-4 py-2 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold hover:scale-[1.02] active:scale-95 transition-transform"
                >
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
                        <span className={`text-[11px] font-black px-3 py-1 rounded-full ${AWARD_LEVEL_COLOR[a.level]}`}>
                          {AWARD_LEVEL_LABEL[a.level]}
                        </span>
                        <button
                          onClick={async () => { await deleteAward(a.id); loadAwards() }}
                          className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg bg-error-container dark:bg-error/20 text-error transition-all"
                        >
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

      {/* Award form modal */}
      {showAwardForm && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => { if (e.target === e.currentTarget) setShowAwardForm(false) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>
                {t('academic.addAward')}
              </h3>
              <button onClick={() => setShowAwardForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form className="p-6 space-y-4" onSubmit={async e => {
              e.preventDefault()
              try { await createAward(awardForm); setShowAwardForm(false); loadAwards() }
              catch { alert('저장 중 오류가 발생했습니다.') }
            }}>
              {[
                { key:'title',        label: t('academic.awardTitle'), placeholder: '예: 교내 프로그래밍 대회' },
                { key:'organization', label: t('academic.awardOrg'),   placeholder: '예: 컴퓨터정보과' },
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
                    {AWARD_LEVELS.map(l => <option key={l} value={l}>{AWARD_LEVEL_LABEL[l]}</option>)}
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
                <button type="button" onClick={() => setShowAwardForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  {t('common.cancel')}
                </button>
                <button type="submit" className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {t('common.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
