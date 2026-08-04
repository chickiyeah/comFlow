import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { generateRoadmap } from '../api/roadmap'
import {
  getActivities, getActivitySummary, createActivity, updateActivity, deleteActivity,
  getSavedJobs, saveJob, deleteSavedJob,
  searchJobs, getCertSchedules,
  searchQualifications, getQualificationDetail, getExamLocations,
  getJobStatistics, refreshImportedJobs,
} from '../api/career'
import { getMyAlerts, createAlert, deleteAlert } from '../api/jobalert'
import { getProfile, updateDesiredJob } from '../api/profile'
import { createResume, generateResumeForJob } from '../api/resume'
import JobPilotWizard from '../components/career/JobPilotWizard'

const CERT_TYPE_STYLE = {
  REQUIRED:    'bg-error-container dark:bg-error/20 text-error border-error/30',
  RECOMMENDED: 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed border-secondary-fixed/30',
  OPTIONAL:    'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 border-outline-variant',
}
const CERT_TYPE_LABEL_KEY = { REQUIRED: 'required', RECOMMENDED: 'recommended', OPTIONAL: 'optional' }

// label is the Korean key used by the backend activity summary map (a.label / actSummary[label]); labelKey is for display
const ACTIVITY_TYPES = [
  { value: 'CERTIFICATE', label: '자격증', labelKey: 'actType_certificate', icon: 'workspace_premium', color: 'text-yellow-500' },
  { value: 'LANGUAGE_TEST', label: '어학시험', labelKey: 'actType_languageTest', icon: 'translate', color: 'text-blue-500' },
  { value: 'INTERNSHIP', label: '인턴십', labelKey: 'actType_internship', icon: 'work', color: 'text-green-500' },
  { value: 'CONTEST', label: '공모전/대회', labelKey: 'actType_contest', icon: 'emoji_events', color: 'text-orange-500' },
  { value: 'VOLUNTEER', label: '봉사활동', labelKey: 'actType_volunteer', icon: 'volunteer_activism', color: 'text-pink-500' },
  { value: 'STUDY_GROUP', label: '스터디', labelKey: 'actType_studyGroup', icon: 'groups', color: 'text-purple-500' },
  { value: 'TRAINING', label: '교육/연수', labelKey: 'actType_training', icon: 'school', color: 'text-teal-500' },
  { value: 'OTHER', label: '기타', labelKey: 'actType_other', icon: 'category', color: 'text-slate-500' },
]

const STATUS_STYLE = {
  PLANNING:    'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300',
  IN_PROGRESS: 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300',
  COMPLETED:   'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  FAILED:      'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
}
const STATUS_LABEL_KEY = {
  PLANNING: 'status_planning', IN_PROGRESS: 'status_inProgress', COMPLETED: 'status_completed', FAILED: 'status_failed',
}

// region/career/empType chip definitions — value is sent to API (Korean), key is for display
const REGIONS = [
  { value: '서울', key: 'region_seoul' }, { value: '경기', key: 'region_gyeonggi' },
  { value: '인천', key: 'region_incheon' }, { value: '부산', key: 'region_busan' },
  { value: '대구', key: 'region_daegu' }, { value: '대전', key: 'region_daejeon' },
  { value: '광주', key: 'region_gwangju' }, { value: '울산', key: 'region_ulsan' },
  { value: '세종', key: 'region_sejong' }, { value: '강원', key: 'region_gangwon' },
  { value: '충북', key: 'region_chungbuk' }, { value: '충남', key: 'region_chungnam' },
  { value: '전북', key: 'region_jeonbuk' }, { value: '전남', key: 'region_jeonnam' },
  { value: '경북', key: 'region_gyeongbuk' }, { value: '경남', key: 'region_gyeongnam' },
  { value: '제주', key: 'region_jeju' },
]
const CAREER_OPTIONS = [
  { value: '신입', key: 'career_new' },
  { value: '경력', key: 'career_experienced' },
  { value: '경력무관', key: 'career_any' },
]
const EMP_TYPES = [
  { value: '전체', key: 'empType_all' },
  { value: '정규직', key: 'empType_fullTime' },
  { value: '계약직', key: 'empType_contract' },
]
const SOURCES = [
  { value: 'all', key: 'source_all' },
  { value: 'jobkorea', key: 'source_jobkorea' },
  { value: 'work24', key: 'source_work24' },
  { value: 'worknet', key: 'source_worknet' },
  { value: 'saramin', key: 'source_saramin' },
  { value: 'imported', key: 'source_imported' },
]

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

  // 공고 맞춤 이력서 초안 state
  const [jobResumeDraft, setJobResumeDraft] = useState(null)   // { draft, matchReport, company, position }
  const [jobResumeLoading, setJobResumeLoading] = useState(false)

  // Job search state
  const [jobKeyword, setJobKeyword] = useState('IT')
  const [jobResults, setJobResults] = useState([])
  const [jobSearchLoading, setJobSearchLoading] = useState(false)
  const [jobFilters, setJobFilters] = useState({ source: 'all', region: '', career: '', empType: '', hideExpired: true })
  const [importRefreshing, setImportRefreshing] = useState(false)
  const [importMsg, setImportMsg] = useState('')
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

  // 취업 통계 state
  const [statJob, setStatJob]         = useState('')
  const [savedDesiredJob, setSavedDesiredJob] = useState('')
  const [statLoading, setStatLoading] = useState(false)
  const [statResult, setStatResult]   = useState(null)
  const [statError, setStatError]     = useState('')

  useEffect(() => {
    if (tab === 'activities') loadActivities()
    if (tab === 'jobs') loadSavedJobs()
    if (tab === 'alerts') loadAlerts()
    if (tab === 'stats') loadDesiredJob()
  }, [tab])

  // 통계 탭 진입 시 저장된 희망직무 프리필 + 즉시 분석
  const loadDesiredJob = async () => {
    if (savedDesiredJob || statJob) return
    try {
      const r = await getProfile()
      const dj = r.data?.desiredJob ?? ''
      if (dj) {
        setSavedDesiredJob(dj)
        setStatJob(dj)
        runStatistics(dj, false)
      }
    } catch { /* ignore */ }
  }

  const runStatistics = async (jobTitle, persist = true) => {
    const job = (jobTitle ?? statJob).trim()
    if (!job) return
    setStatLoading(true)
    setStatError('')
    setStatResult(null)
    try {
      if (persist && job !== savedDesiredJob) {
        try { await updateDesiredJob(job); setSavedDesiredJob(job) } catch { /* 저장 실패해도 통계는 진행 */ }
      }
      const res = await getJobStatistics(job)
      setStatResult(res.data)
    } catch {
      setStatError(t('career.statError'))
    } finally {
      setStatLoading(false)
    }
  }
  const handleStatistics = (e) => { e.preventDefault(); runStatistics() }

  const loadAlerts = async () => {
    setAlertsLoading(true)
    try { const r = await getMyAlerts(); setAlerts(r.data ?? []) }
    catch { setAlerts([]) }
    finally { setAlertsLoading(false) }
  }
  const handleCreateAlert = async (e) => {
    e.preventDefault()
    try { await createAlert(alertForm); setShowAlertForm(false); setAlertForm({ jobTitle:'', region:'', keyword:'' }); loadAlerts() }
    catch { alert(t('career.alertCreateFailed')) }
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
      setRoadmapError(t('career.roadmapError'))
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
    if (!confirm(t('career.confirmDelete'))) return
    await deleteActivity(id)
    loadActivities()
  }

  // Job search
  const handleJobSearch = async (e) => {
    e?.preventDefault()
    setJobSearchLoading(true)
    setJobResults([])
    try {
      const params = { hideExpired: jobFilters.hideExpired }
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

  // 공공 채용공고 수동 수집 트리거 (백그라운드 적재 즉시 갱신)
  const handleImportRefresh = async () => {
    setImportRefreshing(true)
    setImportMsg('')
    try {
      const res = await refreshImportedJobs()
      setImportMsg(res.message || t('career.importDone'))
      if (jobFilters.source === 'imported') handleJobSearch()
    } catch {
      setImportMsg(t('career.importFail'))
    } finally { setImportRefreshing(false) }
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
    alert(t('career.jobSaved'))
  }
  const handleDeleteSavedJob = async (id) => {
    await deleteSavedJob(id)
    loadSavedJobs()
  }

  // 공고 맞춤 이력서 초안 생성 (jobType: 'saved' | 'imported')
  const handleJobResume = async (jobType, jobId) => {
    setJobResumeLoading(true)
    try {
      const res = await generateResumeForJob({ jobType, jobId, template: 'general' })
      setJobResumeDraft(res.data)
    } catch {
      alert(t('career.jobResumeError'))
    } finally {
      setJobResumeLoading(false)
    }
  }

  // 초안을 실제 이력서로 저장
  const handleSaveJobResume = async () => {
    if (!jobResumeDraft) return
    const d = jobResumeDraft.draft?.data ?? {}
    const coverText = (d.coverLetter || []).map(s => `[${s.question}]\n${s.body}`).join('\n\n')
    const skillsCsv = (d.skills || []).flatMap(g => g.items || []).join(', ')
    try {
      await createResume({
        title: `${jobResumeDraft.company} ${jobResumeDraft.position} 지원 이력서`,
        summary: coverText,
        skills: skillsCsv,
        targetJob: d.targetJob || jobResumeDraft.position || '',
        resumeData: JSON.stringify(d),
        template: jobResumeDraft.draft?.template || 'general',
        portfolioIds: [],
      })
      setJobResumeDraft(null)
      alert(t('career.jobResumeSaved'))
    } catch {
      alert(t('career.jobResumeError'))
    }
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
    { key: 'roadmap',    labelKey: 'roadmap',      icon: 'route' },
    { key: 'jobpilot',   labelKey: 'tab_jobpilot',   icon: 'auto_awesome' },
    { key: 'stats',      labelKey: 'tab_stats',      icon: 'insights' },
    { key: 'activities', labelKey: 'tab_activities', icon: 'checklist' },
    { key: 'jobs',       labelKey: 'tab_jobs',       icon: 'work_outline' },
    { key: 'certs',      labelKey: 'certificates',   icon: 'workspace_premium' },
    { key: 'alerts',     labelKey: 'tab_alerts',     icon: 'notifications_active' },
  ]

  return (
    <Layout title={t('career.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('career.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('career.subtitle')}</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 bg-surface-container dark:bg-slate-800 p-1 rounded-2xl w-fit">
        {TABS.map(({ key, labelKey, icon }) => (
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
            {t(`career.${labelKey}`)}
          </button>
        ))}
      </div>

      {/* ── Roadmap Tab ── */}
      {tab === 'roadmap' && (
        <>
          <div className="card p-6 mb-6">
            <form onSubmit={handleRoadmap} className="space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">{t('career.desiredJob')}</label>
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
                  <p className="font-bold text-sm text-primary dark:text-white">{t('career.aiMode')}</p>
                  <p className="text-label-md text-outline dark:text-slate-400">
                    {useExternal ? t('career.aiModeExternalDesc') : t('career.aiModeInternalDesc')}
                  </p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={useExternal}
                  onClick={() => setUseExternal(v => !v)}
                  style={{ minWidth: 52 }}
                  className={`relative inline-flex h-7 w-13 shrink-0 items-center rounded-full transition-colors duration-200 focus:outline-none ${useExternal ? 'bg-secondary dark:bg-secondary-fixed' : 'bg-slate-300 dark:bg-slate-600'}`}
                >
                  <span className={`inline-block h-5 w-5 rounded-full bg-white shadow-sm transform transition-transform duration-200 ${useExternal ? 'translate-x-7' : 'translate-x-1'}`} />
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
                  <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">{t('career.roadmapTitle', { jobTitle: roadmapResult.jobTitle })}</p>
                  <p className="text-label-md text-outline dark:text-slate-400">{t('career.aiSourceLabel')}: {roadmapResult.aiSource === 'EXTERNAL' ? t('career.aiSourceExternal') : t('career.aiSourceInternal')}</p>
                </div>
              </div>
              <div className="card p-6">
                <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>{t('career.recommendedCerts')}
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  {roadmapResult.certificates?.map((c, i) => (
                    <div key={i} className={`p-4 rounded-xl border ${CERT_TYPE_STYLE[c.type] ?? CERT_TYPE_STYLE.OPTIONAL}`}>
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <p className="font-bold text-sm">{c.name}</p>
                        <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-white/50 dark:bg-black/20 shrink-0">{t(`career.${CERT_TYPE_LABEL_KEY[c.type] ?? 'optional'}`)}</span>
                      </div>
                      <p className="text-[12px] opacity-80">{c.description}</p>
                    </div>
                  ))}
                </div>
              </div>
              <div className="card p-6">
                <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">timeline</span>{t('career.learningRoadmap')}
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {roadmapResult.semesterPlans?.map((p, i) => (
                    <div key={i} className="bg-surface-container-low dark:bg-slate-800 p-5 rounded-2xl border border-outline-variant dark:border-slate-700 relative overflow-hidden">
                      <div className="absolute top-0 right-0 text-[80px] font-black text-primary/5 dark:text-white/5 font-['Space_Grotesk'] -mt-2 -mr-2">{p.semester}</div>
                      <div className="relative z-10">
                        <div className="flex items-center gap-2 mb-3">
                          <span className="w-7 h-7 rounded-lg bg-primary dark:bg-primary-container flex items-center justify-center text-white text-xs font-black">{p.semester}</span>
                          <span className="font-bold text-primary dark:text-white">{t('career.semesterLabel', { semester: p.semester })}</span>
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
              <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('career.roadmapEmptyTitle')}</p>
              <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2">{t('career.roadmapEmptySubtitle')}</p>
            </div>
          )}
        </>
      )}

      {/* ── JobPilot Tab (채용공고 → 직무 맞춤 자소서) ── */}
      {tab === 'jobpilot' && <JobPilotWizard />}

      {/* ── 취업 통계 Tab ── */}
      {tab === 'stats' && (
        <>
          <div className="card p-6 mb-6">
            <form onSubmit={handleStatistics} className="space-y-2">
              <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">{t('career.desiredJob')}</label>
              <div className="flex gap-3">
                <input
                  value={statJob}
                  onChange={e => setStatJob(e.target.value)}
                  placeholder={t('career.jobPlaceholder')}
                  className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 transition-all"
                />
                <button
                  type="submit"
                  disabled={statLoading || !statJob.trim()}
                  className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                >
                  {statLoading ? t('career.statAnalyzing') : t('career.statAnalyze')}
                </button>
              </div>
              <p className="text-label-md text-outline dark:text-slate-500">{t('career.statHint')}</p>
            </form>
            {statError && (
              <p className="mt-4 text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{statError}</p>
            )}
          </div>

          {statLoading && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {[1,2,3].map(i => (
                  <div key={i} className="card p-6 animate-pulse">
                    <div className="h-3 bg-surface-container dark:bg-slate-700 rounded w-1/2 mb-3" />
                    <div className="h-6 bg-surface-container dark:bg-slate-700 rounded w-2/3" />
                  </div>
                ))}
              </div>
            </div>
          )}

          {statResult && !statLoading && (
            <div className="space-y-6">
              {/* 헤더 */}
              <div className="flex items-center gap-3 p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-3xl">insights</span>
                <div>
                  <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">{t('career.statTitle', { jobTitle: statResult.jobTitle })}</p>
                  <p className="text-label-md text-outline dark:text-slate-400">{t('career.statAnalyzedCount', { count: statResult.totalPostings })}</p>
                </div>
              </div>

              {/* 핵심 지표 카드 */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="card p-5">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-xl">payments</span>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">{t('career.statExpectedSalary')}</p>
                  </div>
                  <p className="font-black text-xl text-primary dark:text-white">{statResult.aiInsight?.expectedSalary ?? '—'}</p>
                  {statResult.salary?.sampleCount > 0 && (
                    <p className="text-[11px] text-outline dark:text-slate-500 mt-1">
                      {t('career.statSalaryActual', {
                        min: statResult.salary.minManwon,
                        max: statResult.salary.maxManwon,
                        avg: statResult.salary.avgManwon,
                        count: statResult.salary.sampleCount,
                      })}
                    </p>
                  )}
                </div>
                <div className="card p-5">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-xl">school</span>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">{t('career.statRequiredEducation')}</p>
                  </div>
                  <p className="font-black text-xl text-primary dark:text-white">{statResult.aiInsight?.requiredEducation ?? '—'}</p>
                </div>
                <div className="card p-5">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-xl">work_outline</span>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">{t('career.statAnalyzedPostings')}</p>
                  </div>
                  <p className="font-black text-xl text-primary dark:text-white">{statResult.totalPostings}{t('career.statCountUnit')}</p>
                </div>
              </div>

              {/* AI 시장 정보 */}
              {statResult.aiInsight && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                  <div className="card p-6">
                    <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                      <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">code</span>{t('career.statCoreSkills')}
                    </h3>
                    <div className="flex flex-wrap gap-2">
                      {(statResult.aiInsight.coreSkills ?? []).map((s, i) => (
                        <span key={i} className="px-3 py-1.5 rounded-full bg-surface-container dark:bg-slate-800 text-sm text-on-surface dark:text-slate-200 border border-outline-variant dark:border-slate-700">{s}</span>
                      ))}
                      {(statResult.aiInsight.coreSkills ?? []).length === 0 && <span className="text-sm text-outline dark:text-slate-500">—</span>}
                    </div>
                  </div>
                  <div className="card p-6">
                    <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
                      <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>{t('career.statRecommendedCerts')}
                    </h3>
                    <div className="flex flex-wrap gap-2">
                      {(statResult.aiInsight.recommendedCerts ?? []).map((c, i) => (
                        <span key={i} className="px-3 py-1.5 rounded-full bg-secondary-container dark:bg-secondary-fixed/20 text-sm text-on-secondary-container dark:text-secondary-fixed border border-secondary-fixed/30">{c}</span>
                      ))}
                      {(statResult.aiInsight.recommendedCerts ?? []).length === 0 && <span className="text-sm text-outline dark:text-slate-500">—</span>}
                    </div>
                  </div>
                </div>
              )}

              {statResult.aiInsight?.outlook && (
                <div className="card p-6">
                  <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">trending_up</span>{t('career.statOutlook')}
                  </h3>
                  <p className="text-sm text-on-surface dark:text-slate-300 leading-relaxed">{statResult.aiInsight.outlook}</p>
                </div>
              )}

              {/* 실제 공고 기반 분포 */}
              {statResult.totalPostings > 0 && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
                  <DistCard title={t('career.statTopCompanies')} icon="apartment" items={statResult.topCompanies} t={t} />
                  <DistCard title={t('career.statRegionDist')} icon="location_on" items={statResult.regionDist} t={t} />
                  <DistCard title={t('career.statCareerDist')} icon="badge" items={statResult.careerDist} t={t} />
                </div>
              )}
            </div>
          )}

          {!statResult && !statLoading && !statError && (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">insights</span>
              <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('career.statEmptyTitle')}</p>
              <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2">{t('career.statEmptySubtitle')}</p>
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
                    <p className="text-xs text-outline dark:text-slate-400">{t(`career.${t2.labelKey}`)}</p>
                    <p className="font-black text-xl text-primary dark:text-white font-['Space_Grotesk']">{actSummary[t2.label]}</p>
                  </div>
                </div>
              ))}
              <div className="bg-secondary-fixed/10 dark:bg-secondary-fixed/10 rounded-xl border border-secondary-fixed/30 p-3 flex items-center gap-3">
                <span className="material-symbols-outlined text-[22px] text-secondary-fixed">bar_chart</span>
                <div>
                  <p className="text-xs text-outline dark:text-slate-400">{t('career.totalActivities')}</p>
                  <p className="font-black text-xl text-primary dark:text-white font-['Space_Grotesk']">
                    {Object.values(actSummary).reduce((a, b) => a + b, 0)}
                  </p>
                </div>
              </div>
            </div>
          )}
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('career.activitiesSubtitle')}</p>
            <button
              onClick={openCreateAct}
              className="flex items-center gap-2 px-4 py-2 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.02] active:scale-95 transition-transform"
            >
              <span className="material-symbols-outlined text-[18px]">add</span>{t('career.addActivity')}
            </button>
          </div>

          {/* Type summary chips */}
          <div className="flex flex-wrap gap-2 mb-5">
            {ACTIVITY_TYPES.map(({ value, labelKey, icon, color }) => {
              const count = activities.filter(a => a.type === value).length
              return (
                <div key={value} className="flex items-center gap-1.5 px-3 py-1.5 bg-surface-container dark:bg-slate-800 rounded-full text-sm">
                  <span className={`material-symbols-outlined text-[16px] ${color}`}>{icon}</span>
                  <span className="text-on-surface dark:text-slate-300 font-medium">{t(`career.${labelKey}`)}</span>
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
              <p className="font-bold text-primary dark:text-white">{t('career.activitiesEmptyTitle')}</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('career.activitiesEmptySubtitle')}</p>
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
                          {t(`career.${STATUS_LABEL_KEY[a.status] ?? 'status_planning'}`)}
                        </span>
                        <span className="text-[11px] text-outline dark:text-slate-500">{t(`career.${typeInfo.labelKey}`)}</span>
                      </div>
                      <div className="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-on-surface-variant dark:text-slate-400">
                        {a.organization && <span>{a.organization}</span>}
                        {a.targetDate && <span>{t('career.targetDate')}: {a.targetDate}</span>}
                        {a.score && <span>{t('career.scoreLabel')}: {a.score}</span>}
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
                  {editingAct ? t('career.editActivity') : t('career.addActivity')}
                </h3>
                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formType')}</label>
                      <select value={actForm.type} onChange={e => setActForm(f => ({...f, type: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none">
                        {ACTIVITY_TYPES.map(at => <option key={at.value} value={at.value}>{t(`career.${at.labelKey}`)}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formStatus')}</label>
                      <select value={actForm.status} onChange={e => setActForm(f => ({...f, status: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none">
                        {Object.entries(STATUS_LABEL_KEY).map(([v, k]) => <option key={v} value={v}>{t(`career.${k}`)}</option>)}
                      </select>
                    </div>
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formTitle')}</label>
                    <input value={actForm.title} onChange={e => setActForm(f => ({...f, title: e.target.value}))}
                      placeholder={t('career.formTitlePlaceholder')} className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formOrganization')}</label>
                    <input value={actForm.organization} onChange={e => setActForm(f => ({...f, organization: e.target.value}))}
                      placeholder={t('career.formOrganizationPlaceholder')} className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.targetDate')}</label>
                      <input type="date" value={actForm.targetDate} onChange={e => setActForm(f => ({...f, targetDate: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                    </div>
                    <div>
                      <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.completedDate')}</label>
                      <input type="date" value={actForm.completedDate} onChange={e => setActForm(f => ({...f, completedDate: e.target.value}))}
                        className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formScore')}</label>
                    <input value={actForm.score} onChange={e => setActForm(f => ({...f, score: e.target.value}))}
                      placeholder={t('career.formScorePlaceholder')} className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
                  </div>
                  <div>
                    <label className="text-xs text-on-surface-variant dark:text-slate-400 block mb-1">{t('career.formMemo')}</label>
                    <textarea value={actForm.memo} onChange={e => setActForm(f => ({...f, memo: e.target.value}))}
                      rows={2} placeholder={t('career.formMemoPlaceholder')} className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none resize-none" />
                  </div>
                </div>
                <div className="flex gap-3 mt-5">
                  <button onClick={() => setShowActModal(false)}
                    className="flex-1 py-2.5 border border-outline-variant dark:border-slate-700 dark:text-slate-300 rounded-xl text-sm font-semibold hover:bg-surface-container transition-colors">
                    {t('career.cancel')}
                  </button>
                  <button onClick={handleSaveAct} disabled={!actForm.title.trim()}
                    className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.01] active:scale-95 transition-transform disabled:opacity-50">
                    {t('career.save')}
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
                placeholder={t('career.jobKeywordPlaceholder')}
                className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
              <button type="submit" disabled={jobSearchLoading}
                className="px-5 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-semibold text-sm shadow hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50 shrink-0">
                {jobSearchLoading ? t('career.searching') : t('career.search')}
              </button>
            </form>

            {/* 지역 */}
            <div>
              <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">{t('career.regionLabel')}</p>
              <div className="flex flex-wrap gap-1.5">
                {REGIONS.map(r => (
                  <button key={r.value} type="button" onClick={() => setFilter('region', r.value)}
                    className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                      jobFilters.region === r.value
                        ? 'bg-primary dark:bg-primary-container text-white shadow'
                        : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                    }`}>{t(`career.${r.key}`)}</button>
                ))}
              </div>
            </div>

            <div className="flex flex-wrap gap-6">
              {/* 경력 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">{t('career.careerLabel')}</p>
                <div className="flex gap-1.5">
                  {CAREER_OPTIONS.map(c => (
                    <button key={c.value} type="button" onClick={() => setFilter('career', c.value === '경력무관' ? '' : c.value)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        (c.value === '경력무관' ? jobFilters.career === '' : jobFilters.career === c.value)
                          ? 'bg-secondary-fixed text-on-secondary-fixed shadow-lime'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{t(`career.${c.key}`)}</button>
                  ))}
                </div>
              </div>

              {/* 고용형태 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">{t('career.empTypeLabel')}</p>
                <div className="flex gap-1.5">
                  {EMP_TYPES.map(et => (
                    <button key={et.value} type="button" onClick={() => setFilter('empType', et.value === '전체' ? '' : et.value)}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        (et.value === '전체' ? jobFilters.empType === '' : jobFilters.empType === et.value)
                          ? 'bg-secondary-fixed text-on-secondary-fixed shadow-lime'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{t(`career.${et.key}`)}</button>
                  ))}
                </div>
              </div>

              {/* 출처 */}
              <div>
                <p className="text-xs text-on-surface-variant dark:text-slate-500 mb-2 font-medium">{t('career.sourceLabel')}</p>
                <div className="flex gap-1.5">
                  {SOURCES.map(s => (
                    <button key={s.value} type="button"
                      onClick={() => setJobFilters(f => ({ ...f, source: s.value }))}
                      className={`px-3 py-1 rounded-full text-xs font-medium transition-all ${
                        jobFilters.source === s.value
                          ? 'bg-tertiary-container text-on-tertiary-container shadow'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>{t(`career.${s.key}`)}</button>
                  ))}
                </div>
              </div>
            </div>

            {/* 마감 필터 + 실시간 수집 */}
            <div className="flex items-center gap-3 flex-wrap pt-1 border-t border-outline-variant/30 dark:border-slate-700">
              <button type="button" role="switch" aria-checked={jobFilters.hideExpired}
                onClick={() => setJobFilters(f => ({ ...f, hideExpired: !f.hideExpired }))}
                className="flex items-center gap-2 text-xs font-medium text-on-surface-variant dark:text-slate-300">
                <span className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${jobFilters.hideExpired ? 'bg-secondary dark:bg-secondary-fixed' : 'bg-slate-300 dark:bg-slate-600'}`}>
                  <span className={`inline-block h-3.5 w-3.5 rounded-full bg-white shadow transform transition-transform ${jobFilters.hideExpired ? 'translate-x-5' : 'translate-x-1'}`} />
                </span>
                {t('career.hideExpired')}
              </button>

              {jobFilters.source === 'imported' && (
                <button type="button" onClick={handleImportRefresh} disabled={importRefreshing}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-tertiary-container text-on-tertiary-container disabled:opacity-50 ml-auto">
                  <span className={`material-symbols-outlined text-[16px] ${importRefreshing ? 'animate-spin' : ''}`}>{importRefreshing ? 'progress_activity' : 'sync'}</span>
                  {importRefreshing ? t('career.importing') : t('career.importNow')}
                </button>
              )}
              {importMsg && <span className="text-xs text-outline dark:text-slate-400">{importMsg}</span>}
            </div>

            {/* 활성 필터 요약 */}
            {(jobFilters.region || jobFilters.career || jobFilters.empType) && (
              <div className="flex items-center gap-2 flex-wrap pt-1 border-t border-outline-variant/30 dark:border-slate-700">
                <span className="text-xs text-outline dark:text-slate-500">{t('career.appliedFilters')}:</span>
                {jobFilters.region && <span className="chip-active text-xs">{t(`career.${REGIONS.find(r => r.value === jobFilters.region)?.key ?? ''}`)}</span>}
                {jobFilters.career && <span className="chip-active text-xs">{t(`career.${CAREER_OPTIONS.find(c => c.value === jobFilters.career)?.key ?? ''}`)}</span>}
                {jobFilters.empType && <span className="chip-active text-xs">{t(`career.${EMP_TYPES.find(et => et.value === jobFilters.empType)?.key ?? ''}`)}</span>}
                <button onClick={() => setJobFilters(f => ({ ...f, region: '', career: '', empType: '' }))}
                  className="text-xs text-error hover:underline ml-1">{t('career.reset')}</button>
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
                {t('career.searchResults')} {jobTotal !== null && <span className="text-outline dark:text-slate-400 font-normal">({t('career.resultsCount', { count: jobResults.length })})</span>}
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
                        {job.deadline && <span className="text-error">{t('career.deadline')} {job.deadline}</span>}
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
                      {typeof job.id === 'string' && job.id.startsWith('imported-') && (
                        <button type="button" disabled={jobResumeLoading}
                          onClick={() => handleJobResume('imported', Number(job.id.replace('imported-', '')))}
                          className="btn-secondary text-xs px-2.5 py-1.5 whitespace-nowrap">
                          {jobResumeLoading ? t('career.jobResumeLoading') : t('career.makeResume')}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          {!jobSearchLoading && jobResults.length === 0 && jobTotal !== null && (
            <div className="card p-10 text-center mb-6">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600 mb-2">search_off</span>
              <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('career.noJobResults')}</p>
            </div>
          )}

          {/* Saved jobs */}
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold text-primary dark:text-white text-sm">{t('career.savedJobs', { count: savedJobs.length })}</h3>
            <button onClick={() => {
              const title = prompt(t('career.promptCompanyName'))
              if (!title) return
              const company = prompt(t('career.promptCompany'))
              if (!company) return
              saveJob({ title, company, source: '직접입력' }).then(loadSavedJobs)
            }} className="text-xs text-primary dark:text-secondary-fixed flex items-center gap-1 hover:underline">
              <span className="material-symbols-outlined text-[14px]">add</span>{t('career.addManually')}
            </button>
          </div>
          {savedJobs.length === 0 ? (
            <div className="card p-10 text-center">
              <span className="material-symbols-outlined text-[48px] text-outline dark:text-slate-600 mb-2">bookmark_border</span>
              <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('career.noSavedJobs')}</p>
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
                      {job.deadline && <span>{t('career.deadline')}: {job.deadline}</span>}
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
                    <button type="button" disabled={jobResumeLoading}
                      onClick={() => handleJobResume('saved', job.id)}
                      className="btn-secondary text-xs px-2.5 py-1.5 whitespace-nowrap">
                      {jobResumeLoading ? t('career.jobResumeLoading') : t('career.makeResume')}
                    </button>
                    <button onClick={() => handleDeleteSavedJob(job.id)}
                      className="p-1.5 rounded-lg hover:bg-error-container dark:hover:bg-red-900/30 transition-colors">
                      <span className="material-symbols-outlined text-[18px] text-error">delete</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* 공고 맞춤 이력서 초안 모달 */}
          {jobResumeDraft && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => setJobResumeDraft(null)}>
              <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[85vh] overflow-y-auto p-6" onClick={e => e.stopPropagation()}>
                <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-1">
                  {jobResumeDraft.company} · {jobResumeDraft.position}
                </h3>
                {jobResumeDraft.matchReport?.summary && (
                  <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-3">{jobResumeDraft.matchReport.summary}</p>
                )}
                <div className="flex flex-wrap gap-1.5 mb-4">
                  {(jobResumeDraft.matchReport?.matchedSkills ?? []).map(s => (
                    <span key={s} className="chip-active text-xs">{s}</span>
                  ))}
                  {(jobResumeDraft.matchReport?.gaps ?? []).map(g => (
                    <span key={g.skill} className="chip text-xs text-error border-error/30" title={g.severity}>▲ {g.skill}</span>
                  ))}
                </div>
                <div className="space-y-3 mb-4">
                  {(jobResumeDraft.draft?.data?.coverLetter ?? []).map((s, i) => (
                    <div key={i} className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                      <p className="font-semibold text-primary dark:text-white text-sm mb-1">{s.question}</p>
                      <p className="text-sm text-on-surface dark:text-slate-300 whitespace-pre-wrap">{s.body}</p>
                    </div>
                  ))}
                </div>
                <div className="flex gap-3 justify-end pt-2">
                  <button onClick={() => setJobResumeDraft(null)}
                    className="px-4 py-2.5 border border-outline-variant dark:border-slate-700 dark:text-slate-300 rounded-xl text-sm font-semibold hover:bg-surface-container transition-colors">
                    {t('career.close')}
                  </button>
                  <button onClick={handleSaveJobResume}
                    className="px-4 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.01] active:scale-95 transition-transform">
                    {t('career.saveResume')}
                  </button>
                </div>
              </div>
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
                placeholder={t('career.certKeywordPlaceholder')}
                className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
              <button type="submit" disabled={certSearchLoading}
                className="px-5 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-semibold text-sm shadow hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50">
                {certSearchLoading ? t('career.searching') : t('career.search')}
              </button>
            </form>
            <p className="text-xs text-on-surface-variant dark:text-slate-500 mt-2">{t('career.qnetHint')}</p>
          </div>

          {certResults.length === 0 && qualResults.length === 0 && !certSearchLoading ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-3">workspace_premium</span>
              <p className="font-bold text-primary dark:text-white">{t('career.certSearchTitle')}</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('career.certSearchSubtitle')}</p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* 자격증 종목 */}
              {qualResults.length > 0 && (
                <div>
                  <h4 className="font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary-fixed text-[20px]">category</span>
                    {t('career.certCategories', { count: qualResults.length })}
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
                    {t('career.examSchedule', { count: certResults.length })}
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
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.writtenApply')}</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.writtenDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.writtenResult')}</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.writtenResultDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.practicalApply')}</p>
                            <p className="font-medium text-on-surface dark:text-slate-200">{cert.practicalDate || '-'}</p>
                          </div>
                          <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                            <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.practicalResult')}</p>
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
                    <div className="py-10 text-center text-outline dark:text-slate-500">{t('career.loading')}</div>
                  ) : (
                    <>
                      {qualDetail ? (
                        <div className="space-y-3">
                          {qualDetail.engJmNm && (
                            <div className="flex items-baseline gap-2">
                              <span className="text-xs text-outline dark:text-slate-500 min-w-[80px]">{t('career.engName')}</span>
                              <span className="text-sm text-on-surface dark:text-slate-200">{qualDetail.engJmNm}</span>
                            </div>
                          )}
                          {qualDetail.relatedDept && (
                            <div className="flex items-baseline gap-2">
                              <span className="text-xs text-outline dark:text-slate-500 min-w-[80px]">{t('career.relatedDept')}</span>
                              <span className="text-sm text-on-surface dark:text-slate-200">{qualDetail.relatedDept}</span>
                            </div>
                          )}
                          {qualDetail.applyQual && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.applyQual')}</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.applyQual}</p>
                            </div>
                          )}
                          {qualDetail.examMethod && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.examMethod')}</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.examMethod}</p>
                            </div>
                          )}
                          {qualDetail.passStandard && (
                            <div>
                              <p className="text-xs text-outline dark:text-slate-500 mb-1">{t('career.passStandard')}</p>
                              <p className="text-sm text-on-surface dark:text-slate-200 bg-surface-container-low dark:bg-slate-800 p-3 rounded-lg whitespace-pre-line">{qualDetail.passStandard}</p>
                            </div>
                          )}
                          <div className="grid grid-cols-2 gap-3">
                            {qualDetail.feeWritten && (
                              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                                <p className="text-xs text-outline dark:text-slate-500 mb-0.5">{t('career.feeWritten')}</p>
                                <p className="text-sm font-bold text-primary dark:text-secondary-fixed">{qualDetail.feeWritten}</p>
                              </div>
                            )}
                            {qualDetail.feePractical && (
                              <div className="bg-surface-container-low dark:bg-slate-800 p-3 rounded-xl">
                                <p className="text-xs text-outline dark:text-slate-500 mb-0.5">{t('career.feePractical')}</p>
                                <p className="text-sm font-bold text-primary dark:text-secondary-fixed">{qualDetail.feePractical}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-outline dark:text-slate-500 text-center py-4">{t('career.detailUnavailable')}</p>
                      )}

                      {examLocations.length > 0 && (
                        <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
                          <p className="text-sm font-bold text-primary dark:text-white mb-2 flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[18px] text-secondary-fixed">location_on</span>
                            {t('career.examLocations', { count: examLocations.length })}
                          </p>
                          <div className="space-y-1.5 max-h-48 overflow-y-auto">
                            {examLocations.slice(0, 20).map((loc, i) => (
                              <div key={i} className="bg-surface-container-low dark:bg-slate-800 p-2.5 rounded-lg">
                                <p className="text-sm font-medium text-on-surface dark:text-slate-200">{loc.placNm} <span className="text-xs text-outline dark:text-slate-500">({loc.brchNm})</span></p>
                                <p className="text-xs text-outline dark:text-slate-500 mt-0.5">{loc.addr} {loc.telNo && `· ${loc.telNo}`}</p>
                              </div>
                            ))}
                            {examLocations.length > 20 && (
                              <p className="text-xs text-outline dark:text-slate-500 text-center mt-2">{t('career.moreLocations', { count: examLocations.length - 20 })}</p>
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
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('career.alertsTitle')}</h3>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('career.alertsSubtitle')}</p>
            </div>
            <button onClick={() => setShowAlertForm(true)}
              className="btn-primary text-sm flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('career.addAlert')}
            </button>
          </div>

          {alertsLoading ? (
            <div className="flex justify-center py-12"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
          ) : alerts.length === 0 ? (
            <div className="card p-16 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">notifications_active</span>
              <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('career.alertsEmptyTitle')}</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('career.alertsEmptySubtitle')}</p>
              <button onClick={() => setShowAlertForm(true)} className="btn-primary mt-6 mx-auto">
                <span className="material-symbols-outlined text-[18px]">add</span>{t('career.registerAlert')}
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
                  <p className="text-[11px] text-outline dark:text-slate-500 mt-3">{t('career.dailyEmailNote')}</p>
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
                  <span className="material-symbols-outlined text-secondary-fixed">notifications_active</span>{t('career.alertModalTitle')}
                </h3>
                <form onSubmit={handleCreateAlert} className="space-y-4">
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('career.alertJobTitle')}</label>
                    <input required value={alertForm.jobTitle} onChange={e => setAlertForm(p => ({...p, jobTitle: e.target.value}))}
                      placeholder={t('career.alertJobTitlePlaceholder')}
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('career.regionLabel')}</label>
                    <input value={alertForm.region} onChange={e => setAlertForm(p => ({...p, region: e.target.value}))}
                      placeholder={t('career.alertRegionPlaceholder')}
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('career.alertKeyword')}</label>
                    <input value={alertForm.keyword} onChange={e => setAlertForm(p => ({...p, keyword: e.target.value}))}
                      placeholder={t('career.alertKeywordPlaceholder')}
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={() => setShowAlertForm(false)}
                      className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('career.cancel')}</button>
                    <button type="submit" className="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold">{t('career.register')}</button>
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

// 실제 공고 기반 분포 카드 (상위 N개 + 비율 바)
function DistCard({ title, icon, items, t }) {
  const list = items ?? []
  const max = list.reduce((m, x) => Math.max(m, x.count), 0) || 1
  return (
    <div className="card p-6">
      <h3 className="font-['Space_Grotesk'] text-base font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
        <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-xl">{icon}</span>{title}
      </h3>
      {list.length === 0 ? (
        <p className="text-sm text-outline dark:text-slate-500">{t('career.statNoData')}</p>
      ) : (
        <ul className="space-y-2.5">
          {list.map((x, i) => (
            <li key={i}>
              <div className="flex items-center justify-between text-sm mb-1">
                <span className="text-on-surface dark:text-slate-200 truncate pr-2">{x.name}</span>
                <span className="text-outline dark:text-slate-400 shrink-0">{x.count}{t('career.statCountUnit')}</span>
              </div>
              <div className="h-1.5 rounded-full bg-surface-container dark:bg-slate-800 overflow-hidden">
                <div className="h-full rounded-full bg-secondary dark:bg-secondary-fixed" style={{ width: `${(x.count / max) * 100}%` }} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
