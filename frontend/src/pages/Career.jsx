import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { generateRoadmap } from '../api/roadmap'
import {
  getActivities, getActivitySummary, createActivity, updateActivity, deleteActivity,
  getSavedJobs, saveJob, deleteSavedJob,
  searchJobs, getCertSchedules,
  searchQualifications, getQualificationDetail, getExamLocations,
} from '../api/career'
import { getMyAlerts, createAlert, deleteAlert } from '../api/jobalert'

const CERT_TYPE_STYLE = {
  REQUIRED:    'bg-error-container dark:bg-error/20 text-error border-error/30',
  RECOMMENDED: 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed border-secondary-fixed/30',
  OPTIONAL:    'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 border-outline-variant',
}
const CERT_TYPE_LABEL = { REQUIRED: '필수', RECOMMENDED: '권장', OPTIONAL: '선택' }

const ACTIVITY_TYPES = [
  { value: 'CERTIFICATE', label: '자격증', icon: 'workspace_premium', color: 'text-yellow-500' },
  { value: 'LANGUAGE_TEST', label: '어학시험', icon: 'translate', color: 'text-blue-500' },
  { value: 'INTERNSHIP', label: '인턴십', icon: 'work', color: 'text-green-500' },
  { value: 'CONTEST', label: '공모전/대회', icon: 'emoji_events', color: 'text-orange-500' },
  { value: 'VOLUNTEER', label: '봉사활동', icon: 'volunteer_activism', color: 'text-pink-500' },
  { value: 'STUDY_GROUP', label: '스터디', icon: 'groups', color: 'text-purple-500' },
  { value: 'TRAINING', label: '교육/연수', icon: 'school', color: 'text-teal-500' },
  { value: 'OTHER', label: '기타', icon: 'category', color: 'text-slate-500' },
]

const STATUS_STYLE = {
  PLANNING:    'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300',
  IN_PROGRESS: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
  COMPLETED:   'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  FAILED:      'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
}
const STATUS_LABELS = {
  PLANNING: '예정', IN_PROGRESS: '진행중', COMPLETED: '완료', FAILED: '불합격/취소',
}

const EMPTY_FORM = {
  type: 'CERTIFICATE', status: 'PLANNING', title: '', organization: '',
  targetDate: '', completedDate: '', score: '', memo: '',
}

export default function Career() {
  const { t } = useTranslation()
  const [tab, setTab] = useState('roadmap')

  // Roadmap state
  const [jobTitle, setJobTitle] = useState('')
  const [useExternal, setUseExternal] = useState(true)
  const [roadmapLoading, setRoadmapLoading] = useState(false)
  const [roadmapResult, setRoadmapResult] = useState(null)
  const [roadmapError, setRoadmapError] = useState('')

  // Activities state
  const [activities, setActivities] = useState([])
  const [actSummary, setActSummary] = useState({})
  const [actLoading, setActLoading] = useState(false)
  const [showActModal, setShowActModal] = useState(false)
  const [editingAct, setEditingAct] = useState(null)
  const [actForm, setActForm] = useState(EMPTY_FORM)

  // Saved jobs state
  const [savedJobs, setSavedJobs] = useState([])

  // Job search state
  const [jobKeyword, setJobKeyword] = useState('IT')
  const [jobResults, setJobResults] = useState([])
  const [jobSearchLoading, setJobSearchLoading] = useState(false)
  const [jobFilters, setJobFilters] = useState({ source: 'all', region: '', career: '', empType: '' })
  const [jobTotal, setJobTotal] = useState(null)

  // Cert search state
  const [certKeyword, setCertKeyword] = useState('')
  const [certResults, setCertResults] = useState([])
  const [qualResults, setQualResults] = useState([])
  const [certSearchLoading, setCertSearchLoading] = useState(false)
  const [selectedQual, setSelectedQual] = useState(null)
  const [qualDetail, setQualDetail] = useState(null)
  const [examLocations, setExamLocations] = useState([])
  const [qualDetailLoading, setQualDetailLoading] = useState(false)

  // 채용 알리미 state
  const [alerts, setAlerts]           = useState([])
  const [alertsLoading, setAlertsLoading] = useState(false)
  const [alertForm, setAlertForm]     = useState({ jobTitle: '', region: '', keyword: '' })
  const [showAlertForm, setShowAlertForm] = useState(false)

  useEffect(() => {
    if (tab === 'activities') loadActivities()
    if (tab === 'jobs') loadSavedJobs()
    if (tab === 'alerts') loadAlerts()
  }, [tab])

  const loadAlerts = async () => {
    setAlertsLoading(true)
    try { const r = await getMyAlerts(); setAlerts(r.data ?? []) }
    catch { setAlerts([]) }
    finally { setAlertsLoading(false) }
  }
  const handleCreateAlert = async (e) => {
    e.preventDefault()
    try { await createAlert(alertForm); setShowAlertForm(false); setAlertForm({ jobTitle:'', region:'', keyword:'' }); loadAlerts() }
    catch { alert('알리미 등록 실패') }
  }

  const loadActivities = async () => {
    setActLoading(true)
    try {
      const [res, sumRes] = await Promise.all([getActivities(), getActivitySummary()])
      setActivities(res.data ?? [])
      setActSummary(sumRes.data ?? {})
    } catch { /* ignore */ }
    finally { setActLoading(false) }
  }

  const loadSavedJobs = async () => {
    try {
      const res = await getSavedJobs()
      setSavedJobs(res.data ?? [])
    } catch { /* ignore */ }
  }

  // Roadmap
  const handleRoadmap = async (e) => {
    e.preventDefault()
    if (!jobTitle.trim()) return
    setRoadmapLoading(true)
    setRoadmapError('')
    setRoadmapResult(null)
    try {
      const res = await generateRoadmap(jobTitle.trim(), useExternal)
      setRoadmapResult(res.data)
    } catch {
      setRoadmapError('로드맵 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setRoadmapLoading(false)
    }
  }

  // Activity CRUD
  const openCreateAct = () => {
    setEditingAct(null)
    setActForm(EMPTY_FORM)
    setShowActModal(true)
  }
  const openEditAct = (a) => {
    setEditingAct(a)
    setActForm({
      type: a.type, status: a.status, title: a.title,
      organization: a.organization ?? '', targetDate: a.targetDate ?? '',
      completedDate: a.completedDate ?? '', score: a.score ?? '', memo: a.memo ?? '',
    })
    setShowActModal(true)
  }
  const handleSaveAct = async () => {
    const payload = {
      ...actForm,
      targetDate: actForm.targetDate || null,
      completedDate: actForm.completedDate || null,
    }
    try {
      if (editingAct) {
        await updateActivity(editingAct.id, payload)
      } else {
        await createActivity(payload)
      }
      setShowActModal(false)
      loadActivities()
    } catch { /* ignore */ }
  }
  const handleDeleteAct = async (id) => {
    if (!confirm('삭제하시겠습니까?')) return
    await deleteActivity(id)
    loadActivities()
  }

  // Job search
  const handleJobSearch = async (e) => {
    e?.preventDefault()
    setJobSearchLoading(true)
    setJobResults([])
    try {
      const params = {}
      if (jobFilters.source !== 'all') params.source = jobFilters.source
      if (jobFilters.region) params.region = jobFilters.region
      if (jobFilters.career) params.career = jobFilters.career
      if (jobFilters.empType) params.empType = jobFilters.empType
      const res = await searchJobs(jobKeyword, 0, params)
      const data = res.data ?? []
      setJobResults(data)
      setJobTotal(data.length)
    } catch { setJobResults([]) }
    finally { setJobSearchLoading(false) }
  }

  const setFilter = (key, val) =>
    setJobFilters(f => ({ ...f, [key]: f[key] === val ? '' : val }))
  const handleSaveJob = async (job) => {
    await saveJob({
      title: job.title, company: job.company, location: job.location,
      url: job.url, deadline: job.deadline, jobType: job.jobType,
      salary: job.salary, description: job.description, source: job.source,
    })
    loadSavedJobs()
    alert('채용공고를 저장했습니다.')
  }
  const handleDeleteSavedJob = async (id) => {
    await deleteSavedJob(id)
    loadSavedJobs()
  }

  // Cert search — 시험일정 + 자격증 종목 동시 조회
  const handleCertSearch = async (e) => {
    e.preventDefault()
    setCertSearchLoading(true)
    try {
      const [schedRes, qualRes] = await Promise.all([
        getCertSchedules(certKeyword || undefined, new Date().getFullYear()).catch(() => ({ data: [] })),
        certKeyword ? searchQualifications(certKeyword).catch(() => ({ data: [] })) : Promise.resolve({ data: [] }),
      ])
      setCertResults(schedRes.data ?? [])
      setQualResults(qualRes.data ?? [])
    } catch {
      setCertResults([]); setQualResults([])
    }
    finally { setCertSearchLoading(false) }
  }

  const openQualDetail = async (q) => {
    setSelectedQual(q)
    setQualDetail(null)
    setExamLocations([])
    setQualDetailLoading(true)
    try {
      const [detailRes, locRes] = await Promise.all([
        getQualificationDetail(q.jmCd, q.qualgbCd).catch(() => ({ data: [] })),
        getExamLocations().catch(() => ({ data: [] })),
      ])
      setQualDetail((detailRes.data ?? [])[0] ?? null)
      setExamLocations(locRes.data ?? [])
    } finally {
      setQualDetailLoading(false)
    }
  }

  const actTypeInfo = (type) => ACTIVITY_TYPES.find(t => t.value === type) ?? ACTIVITY_TYPES[7]

  const TABS = [
    { key: 'roadmap',    label: '로드맵',    icon: 'route' },
    { key: 'activities', label: '취업 준비', icon: 'checklist' },
    { key: 'jobs',       label: '채용공고',  icon: 'work_outline' },
    { key: 'certs',      label: '자격증',    icon: 'workspace_premium' },
    { key: 'alerts',     label: '채용 알리미', icon: 'notifications_active' },
  ]

  return (
    <Layout title={t('career.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('career.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('career.subtitle')}</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 bg-surface-container dark:bg-slate-800 p-1 rounded-2xl w-fit">
        {TABS.map(({ key, label, icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
              tab === key
                ? 'bg-white dark:bg-slate-700 text-primary dark:text-white shadow'
                : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">{icon}</span>
            {label}
          </button>
        ))}
      </div>

      {/* ── Roadmap Tab ── */}
      {tab === 'roadmap' && (
        <>
          <div className="card p-6 mb-6">
            <form onSubmit={handleRoadmap} className="space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">희망 직무</label>
                <div className="flex gap-3">
                  <input
                    value={jobTitle}
                    onChange={e => setJobTitle(e.target.value)}
                    placeholder={t('career.jobPlaceholder')}
                    className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"
                  />
                  <button
                    type="submit"
                    disabled={roadmapLoading || !jobTitle.trim()}
                    className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                  >
                    {roadmapLoading ? t('career.generating') : t('career.generateBtn')}
                  </button>
                </div>
              </div>
              <div className="flex items-center justify-between p-4 bg-surface-container-low dark:bg-slate-800 rounded-xl border border-outline-variant dark:border-slate-700">
                <div>
                  <p className="font-bold text-sm text-primary dark:text-white">AI 모드</p>
                  <p className="text-label-md text-outline dark:text-slate-400">
                    {useExternal ? '외부 AI — 최신 채용 트렌드 포함 (OpenAI + Claude)' : '내부 AI — 학과 자료만 참고 (ChromaDB RAG)'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setUseExternal(v => !v)}
                  className={`relative w-14 h-7 rounded-full transition-colors ${useExternal ? 'bg-secondary dark:bg-secondary-fixed' : 'bg-outline-variant dark:bg-slate-600'}`}
                >
                  <span className={`absolute top-1 w-5 h-5 bg-white rounded-full shadow transition-transform ${useExternal ? 'translate-x-7' : 'translate-x-1'}`} />
                </button>
              </div>
            </form>
            {roadmapError && (
              <p className="mt-4 text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{roadmapError}</p>
            )}
          </div>

          {roadmapLoading && (
            <div className="space-y-4">
              {[1,2,3].map(i => (
                <div key={i} className="card p-6 animate-pulse">
                  <div className="h-4 bg-surface-container dark:bg-slate-700 rounded w-1/3 mb-3" />
                  <div className="h-3 bg-surface-container dark:bg-slate-700 rounded w-2/3 mb-2" />
                  <div className="h-3 bg-surface-container dark:bg-slate-700 rounded w-1/2" />
                </div>
              ))}
            </div>
          )}

          {roadmapResult && !roadmapLoading && (
            <div className="space-y-6">
              <div className="flex items-center gap-3 p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-3xl">route</span>
                <div>
                  <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">{roadmapResult.jobTitle} 로드맵</p>
                  <p className="text-label-md text-outline dark:text-slate-400">AI 출처: {roadmapResult.aiSource === 'EXTERNAL' ? '외부 AI (최신 트렌드 반영)' : '내부 AI (학과 자료 기반)'}</p>
                </div>
              </div>
              <div className="card p-6">
                <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>추천 자격증
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  {roadmapResult.certificates?.map((c, i) => (
                    <div key={i} className={`p-4 rounded-xl border ${CERT_TYPE_STYLE[c.type] ?? CERT_TYPE_STYLE.OPTIONAL}`}>
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <p className="font-bold text-sm">{c.name}</p>
                        <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-white/50 dark:bg-black/20 shrink-0">{CERT_TYPE_LABEL[c.type]}</span>
                      </div>
                      <p className="text-[12px] opacity-80">{c.description}</p>
                    </div>
                  ))}
                </div>
              </div>
              <div className="card p-6">
                <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">timeline</span>4학기 학습 로드맵
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {roadmapResult.semesterPlans?.map((p, i) => (
                    <div key={i} className="bg-surface-container-low dark:bg-slate-800 p-5 rounded-2xl border border-outline-variant dark:border-slate-700 relative overflow-hidden">
                      <div className="absolute top-0 right-0 text-[80px] font-black text-primary/5 dark:text-white/5 font-['Space_Grotesk'] -mt-2 -mr-2">{p.semester}</div>
                      <div className="relative z-10">
                        <div className="flex items-center gap-2 mb-3">
                          <span className="w-7 h-7 rounded-lg bg-primary dark:bg-primary-container flex items-center justify-center text-white text-xs font-black">{p.semester}</span>
                          <span className="font-bold text-primary dark:text-white">{p.semester}학기</span>
                          <span className="text-label-md text-outline dark:text-slate-400 ml-auto">{p.focus}</span>
                        </div>
                        <ul className="space-y-1.5">
                          {p.tasks?.map((task, j) => (
                            <li key={j} className="flex items-start gap-2 text-sm text-on-surface dark:text-slate-300">
                              <span className="w-1.5 h-1.5 rounded-full bg-secondary dark:bg-secondary-fixed mt-1.5 shrink-0" />
                              {task}
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {!roadmapResult && !roadmapLoading && !roadmapError && (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">work_outline</span>
              <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">희망 직업을 입력해보세요</p>
              <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2">AI가 맞춤형 2년 4학기 학습 경로와 자격증을 추천합니다.</p>
            </div>
          )}
        </>
      )}

      {/* ── Activities Tab ── */}
      {tab === 'activities' && (
        <>
          {/* 활동 요약 통계 */}
          {Object.keys(actSummary).length > 0 && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-5">
              {ACTIVITY_TYPES.filter(t2 => actSummary[t2.label]).map(t2 => (
                <div key={t2.value} className="bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 p-3 flex items-center gap-3 shadow-sm">
                  <span className={`material-symbols-outlined text-[22px] ${t2.color}`}>{t2.icon}</span>
                  <div>
                    <p className="text-xs text-outline dark:text-slate-400">{t2.label}</p>
                    <p className="font-black text-xl text-primary dark:text-white font-['Space_Grotesk']">{actSummary[t2.label]}</p>
                  </div>
                </div>
              ))}
              <div className="bg-secondary-fixed/10 dark:bg-secondary-fixed/10 rounded-xl border border-secondary-fixed/30 p-3 flex items-center gap-3">
                <span className="material-symbols-outlined text-[22px] text-secondary-fixed">bar_chart</span>
                <div>
                  <p className="text-xs text-outline dark:text-slate-400">총 활동</p>
                  <p className="font-black text-xl text-primary dark:text-white font-['Space_Grotesk']">
                    {Object.values(actSummary).reduce((a, b) => a + b, 0)}
                  </p>
                </div>
              </div>
            </div>
          )}
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">취업 준비 활동을 기록하고 관리하세요</p>
            <button
              onClick={openCreateAct}
              className="flex items-center gap-2 px-4 py-2 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.02] active:scale-95 transition-transform"
            >
              <span className="material-symbols-outlined text-[18px]">add</span>활동 추가
            </button>
          </div>

          {/* Type summary chips */}
          <div className="flex flex-wrap gap-2 mb-5">
            {ACTIVITY_TYPES.map(({ value, label, icon, color }) => {
              const count = activities.filter(a => a.type === value).length
              return (
                <div key={value} className="flex items-center gap-1.5 px-3 py-1.5 bg-surface-container dark:bg-slate-800 rounded-full text-sm">
                  <span className={`material-symbols-outlined text-[16px] ${color}`}>{icon}</span>
                  <span className="text-on-surface dark:text-slate-300 font-medium">{label}</span>
                  <span className="text-xs font-black text-primary dark:text-secondary-fixed ml-0.5">{count}</span>
                </div>
              )
            })}
          </div>

          {actLoading ? (
            <div className="space-y-3">
              {[1,2,3].map(i => <div key={i} className="card p-4 animate-pulse h-16" />)}
            </div>
          ) : activities.length === 0 ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-3">checklist</span>
              <p className="font-bold text-primary dark:text-white">아직 등록된 활동이 없습니다</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">자격증, 어학시험, 인턴십 등 취업 준비 활동을 추가해보세요</p>
            </div>
          ) : (
            <div className="space-y-3">
              {activities.map(a => {
                const typeInfo = actTypeInfo(a.type)
                return (
                  <div key={a.id} className="card p-4 flex items-start gap-4">
                    <div className={`w-10 h-10 rounded-xl bg-surface-container dark:bg-slate-800 flex items-center justify-center shrink-0`}>
                      <span className={`material-symbols-outlined ${typeInfo.color}`}>{typeInfo.icon}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-0.5">
                        <span className="font-semibold text-on-surface dark:text-white text-sm">{a.title}</span>
                        <span className={`text-[11px] px-2 py-0.5 rounded-full font-semibold ${STATUS_STYLE[a.status]}`}>
                          {STATUS_LABELS[a.status]}
                        </span>
                        <span className="text-[11px] text-outline dark:text-slate-500">{typeInfo.label}</span>
                      </div>
                      <div className="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-on-surface-variant dark:text-slate-400">
                        {a.organization && <span>{a.organization}</span>}
                        {a.targetDate && <span>목표일: {a.targetDate}</span>}
                        {a.score && <span>점수: {a.score}</span>}
                      </div>
                      {a.memo && <p className="text-xs text-outline dark:text-slate-500 mt-1 truncate">{a.memo}</p>}
                    </div>
                    <div className="flex gap-1 shrink-0">
                      <button onClick={() => openEditAct(a)} className="p-1.5 rounded-lg hover:bg-surface-container dark:hover:bg-slate-700 transition-colors">
                        <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">edit</span>
                      </button>
                      <button onClick={() => handleDeleteAct(a.id)} className="p-1.5 rounded-lg hover:bg-error-container dark:hover:bg-red-900/30 transition-colors">
                        <span className="material-symbols-outlined text-[18px] text-error">delete</span>
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          {/* Activity Modal */}
          {showActModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => setShowActModal(false)}>
              <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
                <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-4">
                  {editingAct ? '활동 수정' : '활동 추가'}
                </h3>
                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">유형</label>
                      <select value={actForm.type} onChange={e => setActForm(f => ({...f, type: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none">
                        {ACTIVITY_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">상태</label>
                      <select value={actForm.status} onChange={e => setActForm(f => ({...f, status: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none">
                        {Object.entries(STATUS_LABELS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                      </select>
                    </div>
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">제목 *</label>
                    <input value={actForm.title} onChange={e => setActForm(f => ({...f, title: e.target.value}))}
                      placeholder="예: 정보처리기사, TOEIC 900" className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">기관/회사</label>
                    <input value={actForm.organization} onChange={e => setActForm(f => ({...f, organization: e.target.value}))}
                      placeholder="주관 기관명" className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">목표일</label>
                      <input type="date" value={actForm.targetDate} onChange={e => setActForm(f => ({...f, targetDate: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                    </div>
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">완료일</label>
                      <input type="date" value={actForm.completedDate} onChange={e => setActForm(f => ({...f, completedDate: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">점수/등급</label>
                    <input value={actForm.score} onChange={e => setActForm(f => ({...f, score: e.target.value}))}
                      placeholder="예: 합격, 870점, 1급" className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">메모</label>
                    <textarea value={actForm.memo} onChange={e => setActForm(f => ({...f, memo: e.target.value}))}
                      rows={2} placeholder="추가 메모" className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none resize-none" />
                  </div>
                </div>
                <div className="flex gap-3 mt-5">
                  <button onClick={() => setShowActModal(false)}
                    className="flex-1 py-2.5 border border-outline-variant dark:border-slate-700 dark:text-slate-300 rounded-xl text-sm font-semibold hover:bg-surface-container transition-colors">
                    취소
                  </button>
                  <button onClick={handleSaveAct} disabled={!actForm.title.trim()}
                    className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.01] active:scale-95 transition-transform disabled:opacity-50">
                    저장
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* ── Jobs Tab ── */}
      {tab === 'jobs' && (
        <>
          {/* Search + 필터 */}
          <div className="card p-5 mb-4 space-y-4">
            <form onSubmit={handleJobSearch} className="flex gap-3">
              <input value={jobKeyword} onChange={e => setJobKeyword(e.target.value)}
                placeholder="키워드 (예: 백엔드, Java, 프론트엔드)"
                className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
              <button type="submit" disabled={jobSearchLoading}
                className="px-5 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-semibold text-sm shadow hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50 shrink-0">
                {jobSearchLoading ? '검색중...' : '검색'}
              </button>
            </form>

            {/* 지역 */}
            <div>
              <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">지역</p>
              <div className="flex flex-wrap gap-1.5">
                {['서울','경기','인천','부산','대구','대전','광주','울산','세종','강원','충북','충남','전북','전남','경북','경남','제주'].map(r => (
                  <button key={r} type="button" onClick={() => setFilter('region', r)}
                    className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                      jobFilters.region === r
                        ? 'bg-primary dark:bg-primary-container text-white shadow'
                        : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                    }`}>{r}</button>
                ))}
              </div>
            </div>

            <div className="flex flex-wrap gap-6">
              {/* 경력 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">경력</p>
                <div className="flex gap-1.5">
                  {['신입','경력','경력무관'].map(c => (
                    <button key={c} type="button" onClick={() => setFilter('career', c === '경력무관' ? '' : c)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        (c === '경력무관' ? jobFilters.career === '' : jobFilters.career === c)
                          ? 'bg-secondary-fixed text-on-secondary-fixed shadow-lime'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{c}</button>
                  ))}
                </div>
              </div>

              {/* 고용형태 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">고용형태</p>
                <div className="flex gap-1.5">
                  {['전체','정규직','계약직'].map(e => (
                    <button key={e} type="button" onClick={() => setFilter('empType', e === '전체' ? '' : e)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        (e === '전체' ? jobFilters.empType === '' : jobFilters.empType === e)
                          ? 'bg-secondary-fixed text-on-secondary-fixed shadow-lime'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{e}</button>
                  ))}
                </div>
              </div>

              {/* 출처 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">출처</p>
                <div className="flex gap-1.5">
                  {[['all','전체'],['jobkorea','잡코리아'],['work24','고용24']].map(([val, label]) => (
                    <button key={val} type="button"
                      onClick={() => setJobFilters(f => ({ ...f, source: val }))}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        jobFilters.source === val
                          ? 'bg-tertiary-container text-on-tertiary-container shadow'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{label}</button>
                  ))}
                </div>
              </div>
            </div>

            {/* 활성 필터 요약 */}
            {(jobFilters.region || jobFilters.career || jobFilters.empType) && (
              <div className="flex items-center gap-2 flex-wrap pt-1 border-t border-outline-variant/30 dark:border-slate-700">
                <span className="text-xs text-outline dark:text-slate-500">적용된 필터:</span>
                {jobFilters.region && <span className="chip-active text-xs">{jobFilters.region}</span>}
                {jobFilters.career && <span className="chip-active text-xs">{jobFilters.career}</span>}
                {jobFilters.empType && <span className="chip-active text-xs">{jobFilters.empType}</span>}
                <button onClick={() => setJobFilters(f => ({ ...f, region: '', career: '', empType: '' }))}
                  className="text-xs text-error hover:underline ml-1">초기화</button>
              </div>
            )}
          </div>

          {/* Search results */}
          {jobSearchLoading && (
            <div className="space-y-3 mb-6">
              {[1,2,3].map(i => <div key={i} className="card p-4 h-16 animate-pulse" />)}
            </div>
          )}
          {!jobSearchLoading && jobResults.length > 0 && (
            <div className="mb-6">
              <h3 className="font-semibold text-primary dark:text-white mb-3 text-sm">
                검색 결과 {jobTotal !== null && <span className="text-outline dark:text-slate-400 font-normal">({jobResults.length}건)</span>}
              </h3>
              <div className="space-y-3">
                {jobResults.map((job, i) => (
                  <div key={i} className="card p-4 flex items-start gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start gap-2 mb-0.5">
                        <p className="font-semibold text-on-surface dark:text-white text-sm leading-snug">{job.title}</p>
                        {job.source && (
                          <span className="shrink-0 text-[10px] px-1.5 py-0.5 rounded-full bg-surface-container dark:bg-slate-700 text-outline dark:text-slate-400 mt-0.5">
                            {job.source}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-x-3 gap-y-0.5 text-xs text-on-surface-variant dark:text-slate-400">
                        <span className="font-medium">{job.company}</span>
                        {job.location && <span>{job.location}</span>}
                        {job.jobType && (
                          <span className="px-1.5 py-0.5 rounded-full bg-surface-container dark:bg-slate-800">
                            {job.jobType}
                          </span>
                        )}
                        {job.salary && <span>{job.salary}</span>}
                        {job.deadline && <span className="text-error">마감 {job.deadline}</span>}
                      </div>
                    </div>
                    <div className="flex gap-1 shrink-0">
                      {job.url && (
                        <a href={job.url} target="_blank" rel="noreferrer"
                          className="p-1.5 rounded-lg hover:bg-surface-container dark:hover:bg-slate-700 transition-colors">
                          <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">open_in_new</span>
                        </a>
                      )}
                      <button onClick={() => handleSaveJob(job)}
                        className="p-1.5 rounded-lg hover:bg-secondary-container dark:hover:bg-secondary-fixed/20 transition-colors">
                        <span className="material-symbols-outlined text-[18px] text-secondary dark:text-secondary-fixed">bookmark_add</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          {!jobSearchLoading && jobResults.length === 0 && jobTotal !== null && (
            <div className="card p-10 text-center mb-6">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600 mb-2">search_off</span>
              <p className="text-sm text-on-surface-variant dark:text-slate-400">검색 결과가 없습니다. 필터를 조정해보세요.</p>
            </div>
          )}

          {/* Saved jobs */}
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-primary dark:text-white text-sm">스크랩한 채용공고 ({savedJobs.length})</h3>
            <button onClick={() => {
              const title = prompt('회사명')
              if (!title) return
              const company = prompt('회사')
              if (!company) return
              saveJob({ title, company, source: '직접입력' }).then(loadSavedJobs)
            }} className="text-xs text-primary dark:text-secondary-fixed flex items-center gap-1 hover:underline">
              <span className="material-symbols-outlined text-[14px]">add</span>직접 추가
            </button>
          </div>
          {savedJobs.length === 0 ? (
            <div className="card p-10 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600 mb-2">bookmark_border</span>
              <p className="text-sm text-on-surface-variant dark:text-slate-400">스크랩한 채용공고가 없습니다</p>
            </div>
          ) : (
            <div className="space-y-3">
              {savedJobs.map(job => (
                <div key={job.id} className="card p-4 flex items-start gap-4">
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-on-surface dark:text-white text-sm mb-0.5">{job.title}</p>
                    <div className="flex flex-wrap gap-x-3 text-xs text-on-surface-variant dark:text-slate-400">
                      <span>{job.company}</span>
                      {job.location && <span>{job.location}</span>}
                      {job.deadline && <span>마감: {job.deadline}</span>}
                      {job.source && <span className="text-outline dark:text-slate-500">{job.source}</span>}
                    </div>
                  </div>
                  <div className="flex gap-1 shrink-0">
                    {job.url && (
                      <a href={job.url} target="_blank" rel="noreferrer"
                        className="p-1.5 rounded-lg hover:bg-surface-container dark:hover:bg-slate-700 transition-colors">
                        <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">open_in_new</span>
                      </a>
                    )}
                    <button onClick={() => handleDeleteSavedJob(job.id)}
                      className="p-1.5 rounded-lg hover:bg-error-container dark:hover:bg-red-900/30 transition-colors">
                      <span className="material-symbols-outlined text-[18px] text-error">delete</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── Certs Tab ── */}
      {tab === 'certs' && (
        <>
          <div className="card p-5 mb-5">
            <form onSubmit={handleCertSearch} className="flex gap-3">
              <input value={certKeyword} onChange={e => setCertKeyword(e.target.value)}
                placeholder="자격증명 검색 (예: 정보처리기사, 컴퓨터활용능력)"
                className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
              <button type="submit" disabled={certSearchLoading}
                className="px-5 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-semibold text-sm shadow hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50">
                {certSearchLoading ? '검색중...' : '검색'}
              </button>
            </form>
            <p className="text-xs text-on-surface-variant dark:text-slate-500 mt-2">큐넷(Q-Net) API 키 등록 시 국가자격 시험 일정을 실시간으로 조회합니다.</p>
          </div>

          {certResults.length === 0 && qualResults.length === 0 && !certSearchLoading ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-3">workspace_premium</span>
              <p className="font-bold text-primary dark:text-white">자격증 검색</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">검색창에 자격증명을 입력하면 시험일정과 종목 정보를 함께 조회합니다</p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* 자격증 종목 */}
              {qualResults.length > 0 && (
                <div>
                  <h4 className="font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary-fixed text-[20px]">category</span>
                    자격증 종목 ({qualResults.length})
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {qualResults.map(q => (
                      <button key={q.jmCd} onClick={() => openQualDetail(q)}
                        className="card p-4 text-left hover:shadow-md hover:border-primary/30 dark:hover:border-secondary-fixed/30 transition-all active:scale-[0.98]">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <p className="font-bold text-on-surface dark:text-white truncate">{q.jmNm}</p>
                            <p className="text-xs text-outline dark:text-slate-500 mt-0.5">{q.qualgbNm} · {q.instiNm}</p>
                          </div>
                          <span className="material-symbols-outlined text-outline dark:text-slate-500 text-[18px] shrink-0">chevron_right</span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* 시험 일정 */}
              {certResults.length > 0 && (
                <div>
                  <h4 className="font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary-fixed text-[20px]">event</span>
                    시험 일정 ({certResults.length})
                  </h4>
                  <div className="space-y-4">
                    {certResults.map((cert, i) => (
                      <div key={i} className="card p-5">
                        <div className="flex items-center gap-2 mb-3">
                          <span className="material-symbols-outlined text-yellow-500">workspace_premium</span>
                          <div>
                            <p className="font-bold text-on-surface dark:text-white">{cert.qualification}</p>
                            <p className="text-xs text-outline dark:text-slate-500">{cert.examName} · {cert.organization}</p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-3 text-sm">
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">필기 원서접수</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.writtenDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">필기 합격발표</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.writtenResultDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">실기 원서접수</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.practicalDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">실기 합격발표</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.practicalResultDate || '-'}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* 자격증 상세 모달 */}
          {selectedQual && (
            <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
              onClick={e => { if (e.target === e.currentTarget) setSelectedQual(null) }}>
              <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
                <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between sticky top-0 bg-white dark:bg-slate-900">
                  <div>
                    <p className="text-xs text-outline dark:text-slate-500">{selectedQual.qualgbNm} · {selectedQual.instiNm}</p>
                    <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mt-0.5">{selectedQual.jmNm}</h3>
                  </div>
                  <button onClick={() => setSelectedQual(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                    <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
                  </button>
                </div>

                <div className="p-6 space-y-4">
                  {qualDetailLoading ? (
                    <div className="py-10 text-center text-outline dark:text-slate-500">불러오는 중…</div>
                  ) : (
                    <>
                      {qualDetail ? (
                        <div className="space-y-3">
                          {qualDetail.engJmNm && (
                            <div className="flex items-baseline gap-2">
                              <span className="text-xs text-outline dark:text-slate-500 min-w-[80px]">영문명</span>
                              <span className="text-sm text-on-surface dark:text-slate-200">{qualDetail.engJmNm}</span>
                            </div>
                          )}
                          {qualDetail.relatedDept && (
                            <div className="flex items-baseline gap-2">
                              <span className="text-xs text-outline dark:text-slate-500 min-w-[80px]">관련 학과</span>
                              <span className="text-sm text-on-surface dark:text-slate-200">{qualDetail.relatedDept}</span>
                            </div>
                          )}
                          {qualDetail.applyQual && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">응시자격</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.applyQual}</p>
                            </div>
                          )}
                          {qualDetail.examMethod && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">검정방법</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.examMethod}</p>
                            </div>
                          )}
                          {qualDetail.passStandard && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">합격기준</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.passStandard}</p>
                            </div>
                          )}
                          <div className="grid grid-cols-2 gap-3">
                            {qualDetail.feeWritten && (
                              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                                <p className="text-xs text-outline dark:text-slate-500 mb-0.5">필기 수수료</p>
                                <p className="text-sm font-bold text-primary dark:text-secondary-fixed">{qualDetail.feeWritten}</p>
                              </div>
                            )}
                            {qualDetail.feePractical && (
                              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                                <p className="text-xs text-outline dark:text-slate-500 mb-0.5">실기 수수료</p>
                                <p className="text-sm font-bold text-primary dark:text-secondary-fixed">{qualDetail.feePractical}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-outline dark:text-slate-500 text-center py-4">상세 정보를 불러올 수 없습니다.</p>
                      )}

                      {examLocations.length > 0 && (
                        <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
                          <p className="text-sm font-bold text-primary dark:text-white mb-2 flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[18px] text-secondary-fixed">location_on</span>
                            전국 시험장소 ({examLocations.length})
                          </p>
                          <div className="space-y-1.5 max-h-48 overflow-y-auto">
                            {examLocations.slice(0, 20).map((loc, i) => (
                              <div key={i} className="bg-surface-container-low dark:bg-slate-800 p-2.5 rounded-lg">
                                <p className="text-sm font-medium text-on-surface dark:text-slate-200">{loc.placNm} <span className="text-xs text-outline dark:text-slate-500">({loc.brchNm})</span></p>
                                <p className="text-xs text-outline dark:text-slate-500 mt-0.5">{loc.addr} {loc.telNo && `· ${loc.telNo}`}</p>
                              </div>
                            ))}
                            {examLocations.length > 20 && (
                              <p className="text-xs text-outline dark:text-slate-500 text-center mt-2">… 외 {examLocations.length - 20}개</p>
                            )}
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* ── 채용 알리미 탭 ── */}
      {tab === 'alerts' && (
        <div>
          <div className="flex justify-between items-center mb-6">
            <div>
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">채용 알리미</h3>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">매일 오전 9시 새 공고를 이메일로 알려드립니다.</p>
            </div>
            <button onClick={() => setShowAlertForm(true)}
              className="btn-primary text-sm flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px]">add</span>알리미 추가
            </button>
          </div>

          {alertsLoading ? (
            <div className="flex justify-center py-12"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
          ) : alerts.length === 0 ? (
            <div className="card p-16 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">notifications_active</span>
              <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">등록된 채용 알리미가 없습니다.</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">관심 직무를 등록하면 매일 새 채용공고를 이메일로 받아볼 수 있습니다.</p>
              <button onClick={() => setShowAlertForm(true)} className="btn-primary mt-6 mx-auto">
                <span className="material-symbols-outlined text-[18px]">add</span>알리미 등록하기
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {alerts.map(a => (
                <div key={a.id} className="card p-5 group">
                  <div className="flex items-start justify-between mb-3">
                    <div className="w-10 h-10 rounded-xl bg-secondary-container/30 dark:bg-secondary-fixed/10 flex items-center justify-center">
                      <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">notifications_active</span>
                    </div>
                    <button onClick={async () => { await deleteAlert(a.id); loadAlerts() }}
                      className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg bg-error-container dark:bg-error/20 text-error transition-all">
                      <span className="material-symbols-outlined text-[15px]">delete</span>
                    </button>
                  </div>
                  <h4 className="font-bold text-primary dark:text-white mb-1">{a.jobTitle}</h4>
                  {a.region && <span className="inline-block text-[11px] px-2 py-0.5 bg-surface-container dark:bg-slate-700 text-outline dark:text-slate-400 rounded mr-1">{a.region}</span>}
                  {a.keyword && <span className="inline-block text-[11px] px-2 py-0.5 bg-surface-container dark:bg-slate-700 text-outline dark:text-slate-400 rounded">{a.keyword}</span>}
                  <p className="text-[11px] text-outline dark:text-slate-500 mt-3">매일 09:00 이메일 발송</p>
                </div>
              ))}
            </div>
          )}

          {/* 알리미 등록 모달 */}
          {showAlertForm && (
            <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
              onClick={e => e.target === e.currentTarget && setShowAlertForm(false)}>
              <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-sm shadow-2xl p-6">
                <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary-fixed">notifications_active</span>채용 알리미 등록
                </h3>
                <form onSubmit={handleCreateAlert} className="space-y-4">
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">직무명 *</label>
                    <input required value={alertForm.jobTitle} onChange={e => setAlertForm(p => ({...p, jobTitle: e.target.value}))}
                      placeholder="예: 백엔드 개발자"
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">지역</label>
                    <input value={alertForm.region} onChange={e => setAlertForm(p => ({...p, region: e.target.value}))}
                      placeholder="예: 서울, 경기"
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">추가 키워드</label>
                    <input value={alertForm.keyword} onChange={e => setAlertForm(p => ({...p, keyword: e.target.value}))}
                      placeholder="예: Spring, React"
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={() => setShowAlertForm(false)}
                      className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">취소</button>
                    <button type="submit" className="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold">등록</button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </Layout>
  )
}
