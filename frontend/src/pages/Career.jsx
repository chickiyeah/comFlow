import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { generateRoadmap } from '../api/roadmap'
import {
  getActivities, createActivity, updateActivity, deleteActivity,
  getSavedJobs, saveJob, deleteSavedJob,
  searchJobs, searchCerts,
} from '../api/career'

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

  // Cert search state
  const [certKeyword, setCertKeyword] = useState('')
  const [certResults, setCertResults] = useState([])
  const [certSearchLoading, setCertSearchLoading] = useState(false)

  useEffect(() => {
    if (tab === 'activities') loadActivities()
    if (tab === 'jobs') loadSavedJobs()
  }, [tab])

  const loadActivities = async () => {
    setActLoading(true)
    try {
      const res = await getActivities()
      setActivities(res.data.data ?? [])
    } catch { /* ignore */ }
    finally { setActLoading(false) }
  }

  const loadSavedJobs = async () => {
    try {
      const res = await getSavedJobs()
      setSavedJobs(res.data.data ?? [])
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
    e.preventDefault()
    setJobSearchLoading(true)
    try {
      const res = await searchJobs(jobKeyword)
      setJobResults(res.data.data ?? [])
    } catch { setJobResults([]) }
    finally { setJobSearchLoading(false) }
  }
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

  // Cert search
  const handleCertSearch = async (e) => {
    e.preventDefault()
    setCertSearchLoading(true)
    try {
      const res = await searchCerts(certKeyword || undefined, new Date().getFullYear())
      setCertResults(res.data.data ?? [])
    } catch { setCertResults([]) }
    finally { setCertSearchLoading(false) }
  }

  const actTypeInfo = (type) => ACTIVITY_TYPES.find(t => t.value === type) ?? ACTIVITY_TYPES[7]

  const TABS = [
    { key: 'roadmap', label: '로드맵', icon: 'route' },
    { key: 'activities', label: '취업 준비', icon: 'checklist' },
    { key: 'jobs', label: '채용공고', icon: 'work_outline' },
    { key: 'certs', label: '자격증 일정', icon: 'workspace_premium' },
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
          {/* Search */}
          <div className="card p-5 mb-5">
            <form onSubmit={handleJobSearch} className="flex gap-3">
              <input value={jobKeyword} onChange={e => setJobKeyword(e.target.value)}
                placeholder="키워드 (예: 백엔드, 프론트엔드, Java)"
                className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm focus:outline-none" />
              <button type="submit" disabled={jobSearchLoading}
                className="px-5 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-semibold text-sm shadow hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50">
                {jobSearchLoading ? '검색중...' : '검색'}
              </button>
            </form>
            <p className="text-xs text-on-surface-variant dark:text-slate-500 mt-2">워크넷 API 키 등록 시 실시간 채용공고를 검색합니다. 키 미등록 시 직접 추가만 가능합니다.</p>
          </div>

          {/* Search results */}
          {jobResults.length > 0 && (
            <div className="mb-6">
              <h3 className="font-semibold text-primary dark:text-white mb-3 text-sm">검색 결과 ({jobResults.length}건)</h3>
              <div className="space-y-3">
                {jobResults.map((job, i) => (
                  <div key={i} className="card p-4 flex items-start gap-4">
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-on-surface dark:text-white text-sm mb-0.5">{job.title}</p>
                      <div className="flex flex-wrap gap-x-3 text-xs text-on-surface-variant dark:text-slate-400">
                        <span>{job.company}</span>
                        {job.location && <span>{job.location}</span>}
                        {job.deadline && <span>마감: {job.deadline}</span>}
                        {job.jobType && <span>{job.jobType}</span>}
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

          {certResults.length === 0 && !certSearchLoading ? (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-3">workspace_premium</span>
              <p className="font-bold text-primary dark:text-white">자격증 시험 일정 조회</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">검색창에 자격증명을 입력하고 검색하세요</p>
            </div>
          ) : (
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
          )}
        </>
      )}
    </Layout>
  )
}
