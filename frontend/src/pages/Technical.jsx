import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import {
  getPortfolios, createPortfolio, updatePortfolio, deletePortfolio,
  generateFromGithub, generateFromFile
} from '../api/portfolio'
import { getResumes, getResume, createResume, updateResume, downloadResumePdf, deleteResume, generateResume } from '../api/resume'
import { generateCoverLetter } from '../api/assistant'
import { getCoverLetters, saveCoverLetter, updateCoverLetter, deleteCoverLetter } from '../api/coverLetter'
import { getGithubTokenStatus, saveGithubToken, deleteGithubToken } from '../api/career'

const STATUS_LABEL = { IN_PROGRESS: 'status_inProgress', COMPLETED: 'status_completed' }
const STATUS_COLOR = {
  IN_PROGRESS: 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed',
  COMPLETED:   'bg-surface-container dark:bg-slate-700 text-on-surface-variant dark:text-slate-300',
}

function PortfolioCard({ p, onDelete, onClick }) {
  const { t } = useTranslation()
  return (
    <div
      onClick={onClick}
      className="card p-5 group relative cursor-pointer hover:shadow-md hover:border-primary/30 dark:hover:border-secondary-fixed/30 transition-all active:scale-[0.98]"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0 mr-8">
          <h4 className="font-bold text-primary dark:text-white truncate group-hover:text-primary dark:group-hover:text-secondary-fixed transition-colors">{p.title}</h4>
          <p className="text-label-md text-outline dark:text-slate-400">{p.role}</p>
        </div>
        <span className={`text-[10px] font-bold px-2 py-1 rounded-full shrink-0 ${STATUS_COLOR[p.status]}`}>
          {t(`technical.${STATUS_LABEL[p.status]}`)}
        </span>
      </div>
      {p.description && (
        <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-3 line-clamp-2">{p.description}</p>
      )}
      {p.techStack?.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-3">
          {p.techStack.slice(0, 5).map(t => (
            <span key={t} className="text-[10px] px-2 py-0.5 bg-surface-container-low dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 rounded-full border border-outline-variant dark:border-slate-700">{t}</span>
          ))}
        </div>
      )}
      <div className="flex items-center gap-2 text-label-md text-outline dark:text-slate-500">
        {p.startDate && <span>{p.startDate} ~ {p.endDate ?? t('technical.present')}</span>}
        {p.githubUrl && (
          <span className="ml-auto flex items-center gap-1 text-primary dark:text-secondary-fixed">
            <span className="material-symbols-outlined text-[14px]">code</span>GitHub
          </span>
        )}
      </div>
      {/* 삭제 버튼 — 클릭 버블 차단 */}
      <button
        onClick={e => { e.stopPropagation(); onDelete(p.id) }}
        className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 p-1.5 rounded-lg bg-error-container dark:bg-error/20 text-error hover:scale-110 transition-all"
      >
        <span className="material-symbols-outlined text-[16px]">delete</span>
      </button>
    </div>
  )
}

function PortfolioDetailModal({ p, onClose, onDelete, onEdit }) {
  const { t } = useTranslation()
  if (!p) return null
  return (
    <div
      className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
        {/* 헤더 */}
        <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between sticky top-0 bg-white dark:bg-slate-900">
          <div className="flex-1 min-w-0 pr-3">
            <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{p.title}</h3>
            <p className="text-label-md text-outline dark:text-slate-400 mt-0.5">{p.role}</p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <span className={`text-[10px] font-bold px-3 py-1.5 rounded-full ${STATUS_COLOR[p.status]}`}>
              {t(`technical.${STATUS_LABEL[p.status]}`)}
            </span>
            <button onClick={() => onEdit(p)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400" title={t('technical.edit')}>
              <span className="material-symbols-outlined text-[20px]">edit</span>
            </button>
            <button onClick={onClose} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
              <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
            </button>
          </div>
        </div>

        <div className="p-6 space-y-5">
          {/* 기간 */}
          {p.startDate && (
            <div className="flex items-center gap-2 text-sm text-on-surface-variant dark:text-slate-400">
              <span className="material-symbols-outlined text-[18px] text-primary dark:text-secondary-fixed">calendar_today</span>
              {p.startDate} ~ {p.endDate ?? t('technical.status_inProgress')}
            </div>
          )}

          {/* 설명 */}
          {p.description && (
            <div>
              <p className="text-label-md text-outline dark:text-slate-500 mb-2">{t('technical.projectDescription')}</p>
              <p className="text-sm text-on-surface dark:text-slate-300 leading-relaxed bg-surface-container-low dark:bg-slate-800 p-4 rounded-xl whitespace-pre-line">
                {p.description}
              </p>
            </div>
          )}

          {/* 기술 스택 */}
          {p.techStack?.length > 0 && (
            <div>
              <p className="text-label-md text-outline dark:text-slate-500 mb-2">{t('technical.techStack')}</p>
              <div className="flex flex-wrap gap-2">
                {p.techStack.map(t => (
                  <span key={t} className="px-3 py-1.5 bg-secondary-container/20 dark:bg-secondary-fixed/10 text-on-secondary-container dark:text-secondary-fixed rounded-full text-sm font-medium border border-secondary-fixed/20">
                    {t}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* 링크 */}
          {(p.githubUrl || p.deployUrl) && (
            <div className="space-y-2">
              {p.githubUrl && (
                <a href={p.githubUrl} target="_blank" rel="noreferrer"
                  className="flex items-center gap-3 p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl hover:bg-surface-container dark:hover:bg-slate-700 transition-colors group">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">code</span>
                  <span className="text-sm text-primary dark:text-secondary-fixed font-medium truncate group-hover:underline">{p.githubUrl}</span>
                  <span className="material-symbols-outlined text-[16px] text-outline dark:text-slate-400 ml-auto shrink-0">open_in_new</span>
                </a>
              )}
              {p.deployUrl && (
                <a href={p.deployUrl} target="_blank" rel="noreferrer"
                  className="flex items-center gap-3 p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl hover:bg-surface-container dark:hover:bg-slate-700 transition-colors group">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">launch</span>
                  <span className="text-sm text-primary dark:text-secondary-fixed font-medium truncate group-hover:underline">{p.deployUrl}</span>
                  <span className="material-symbols-outlined text-[16px] text-outline dark:text-slate-400 ml-auto shrink-0">open_in_new</span>
                </a>
              )}
            </div>
          )}

          {/* 삭제 버튼 */}
          <button
            onClick={() => { onDelete(p.id); onClose() }}
            className="w-full py-3 border border-error/30 text-error rounded-xl text-sm font-bold hover:bg-error-container dark:hover:bg-error/20 transition-colors flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-[18px]">delete</span>{t('technical.deletePortfolio')}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Technical() {
  const { t } = useTranslation()
  const [tab, setTab] = useState('portfolio')
  const [portfolios, setPortfolios] = useState([])
  const [resumes, setResumes] = useState([])
  const [coverLetters, setCoverLetters] = useState([])
  const [loading, setLoading] = useState(false)

  // AI generate state
  const [showGenModal, setShowGenModal] = useState(false)
  const [githubUrl, setGithubUrl] = useState('')
  const [genLoading, setGenLoading] = useState(false)
  const [draft, setDraft] = useState(null)
  const [genError, setGenError] = useState('')
  const fileRef = useRef()

  // Portfolio detail modal
  const [selectedPortfolio, setSelectedPortfolio] = useState(null)
  const [editingPortfolio, setEditingPortfolio] = useState(null)
  const [portfolioEditSaving, setPortfolioEditSaving] = useState(false)

  // GitHub token state
  const [hasGhToken, setHasGhToken] = useState(false)
  const [showTokenModal, setShowTokenModal] = useState(false)
  const [tokenInput, setTokenInput] = useState('')
  const [tokenSaving, setTokenSaving] = useState(false)

  // Resume create state
  const [showResumeForm, setShowResumeForm] = useState(false)
  const [resumeForm, setResumeForm] = useState({ title: '', summary: '', skills: '', targetJob: '', portfolioIds: [], resumeData: '', template: '' })

  // AI 이력서 자동생성 state
  const [resumeGenerating, setResumeGenerating] = useState(false)
  const [honestyFixes, setHonestyFixes] = useState([])

  // Resume edit state
  const [editingResume, setEditingResume] = useState(null)
  const [resumeEditSaving, setResumeEditSaving] = useState(false)

  // 자기소개서 AI 생성 state (이력서 폼 내)
  const [coverLoading, setCoverLoading] = useState(false)
  const [coverCompany, setCoverCompany] = useState('')
  const [coverJobTitle, setCoverJobTitle] = useState('')

  // 자기소개서 탭 state
  const [showCLModal, setShowCLModal] = useState(false)
  const [editingCL, setEditingCL] = useState(null)
  const [clForm, setClForm] = useState({ title: '', companyName: '', jobTitle: '', content: '' })
  const [clSections, setClSections] = useState([])   // 선택된 소제목 목록
  const [clCustomSection, setClCustomSection] = useState('')  // 직접 입력
  const [clAiLoading, setClAiLoading] = useState(false)
  const [selectedCL, setSelectedCL] = useState(null) // 상세보기

  useEffect(() => {
    loadData()
  }, [tab])

  useEffect(() => {
    getGithubTokenStatus().then(r => setHasGhToken(r.data?.hasToken ?? false)).catch(() => {})
  }, [])

  const handleSaveToken = async () => {
    if (!tokenInput.trim()) return
    setTokenSaving(true)
    try {
      await saveGithubToken(tokenInput.trim())
      setHasGhToken(true)
      setShowTokenModal(false)
      setTokenInput('')
    } catch { /* ignore */ }
    finally { setTokenSaving(false) }
  }

  const handleDeleteToken = async () => {
    await deleteGithubToken()
    setHasGhToken(false)
  }

  const loadData = async () => {
    setLoading(true)
    try {
      if (tab === 'portfolio') {
        const r = await getPortfolios()
        setPortfolios(r.data ?? [])
      } else if (tab === 'resume') {
        const r = await getResumes()
        setResumes(r.data ?? [])
      } else {
        const r = await getCoverLetters()
        setCoverLetters(r.data ?? [])
      }
    } catch {
      // ignore — show empty state
    } finally {
      setLoading(false)
    }
  }

  const handleGithubGenerate = async () => {
    if (!githubUrl.trim()) return
    setGenLoading(true)
    setGenError('')
    try {
      const r = await generateFromGithub(githubUrl.trim())
      setDraft(r.data)
    } catch {
      setGenError(t('technical.errGithubFetch'))
    } finally {
      setGenLoading(false)
    }
  }

  const handleFileGenerate = async (file) => {
    setGenLoading(true)
    setGenError('')
    try {
      const r = await generateFromFile(file)
      setDraft(r.data)
    } catch {
      setGenError(t('technical.errFileAnalyze'))
    } finally {
      setGenLoading(false)
    }
  }

  const handleSaveDraft = async () => {
    if (!draft) return
    if (!draft.title?.trim()) { setGenError(t('technical.errEmptyTitle')); return }
    if (!draft.role?.trim()) { setGenError(t('technical.errEmptyRole')); return }
    try {
      await createPortfolio({
        title: draft.title.trim(),
        description: draft.description ?? '',
        role: draft.role.trim(),
        techStack: draft.techStack?.join(', ') ?? '',
        startDate: draft.startDate ?? null,
        endDate: draft.endDate ?? null,
        githubUrl: draft.githubUrl ?? null,
        deployUrl: draft.deployUrl ?? null,
        status: draft.status ?? 'COMPLETED',
      })
      setShowGenModal(false)
      setDraft(null)
      setGithubUrl('')
      loadData()
    } catch (err) {
      setGenError(err?.message || t('technical.errSave'))
    }
  }

  const handleDeletePortfolio = async (id) => {
    if (!confirm(t('technical.confirmDeletePortfolio'))) return
    await deletePortfolio(id)
    setPortfolios(prev => prev.filter(p => p.id !== id))
  }

  const openEditPortfolio = (p) => {
    setEditingPortfolio({
      id: p.id,
      title: p.title ?? '',
      role: p.role ?? '',
      description: p.description ?? '',
      techStack: (p.techStack ?? []).join(', '),
      startDate: p.startDate ?? '',
      endDate: p.endDate ?? '',
      githubUrl: p.githubUrl ?? '',
      deployUrl: p.deployUrl ?? '',
      status: p.status ?? 'IN_PROGRESS',
    })
    setSelectedPortfolio(null)
  }

  const handleSavePortfolioEdit = async (e) => {
    e.preventDefault()
    if (!editingPortfolio) return
    setPortfolioEditSaving(true)
    try {
      const { id, ...rest } = editingPortfolio
      await updatePortfolio(id, {
        ...rest,
        startDate: rest.startDate || null,
        endDate: rest.endDate || null,
        githubUrl: rest.githubUrl || null,
        deployUrl: rest.deployUrl || null,
      })
      setEditingPortfolio(null)
      loadData()
    } catch {
      alert(t('technical.errPortfolioEdit'))
    } finally {
      setPortfolioEditSaving(false)
    }
  }

  const openEditResume = async (id) => {
    try {
      const r = await getResume(id)
      const d = r.data
      setEditingResume({
        id: d.id,
        title: d.title ?? '',
        targetJob: d.targetJob ?? '',
        summary: d.summary ?? '',
        skills: (d.skills ?? []).join(', '),
      })
    } catch {
      alert(t('technical.errResumeLoad'))
    }
  }

  const handleSaveResumeEdit = async (e) => {
    e.preventDefault()
    if (!editingResume) return
    setResumeEditSaving(true)
    try {
      const { id, ...rest } = editingResume
      await updateResume(id, rest)
      setEditingResume(null)
      loadData()
    } catch {
      alert(t('technical.errResumeEdit'))
    } finally {
      setResumeEditSaving(false)
    }
  }

  const handleCreateResume = async (e) => {
    e.preventDefault()
    try {
      await createResume({ ...resumeForm, portfolioIds: portfolios.slice(0, 3).map(p => p.id) })
      setShowResumeForm(false)
      setResumeForm({ title: '', summary: '', skills: '', targetJob: '', portfolioIds: [], resumeData: '', template: '' })
      setHonestyFixes([])
      if (tab === 'resume') loadData()
    } catch {
      alert(t('technical.errResumeCreate'))
    }
  }

  const handleGenerateResume = async (template = 'general') => {
    setResumeGenerating(true)
    try {
      const r = await generateResume(template)
      const draft = r.data ?? {}   // { data, honestyReport, template }
      const d = draft.data ?? {}
      const skillsCsv = (d.skills || []).flatMap(g => g.items || []).join(', ')
      const coverText = (d.coverLetter || [])
        .map(s => `[${s.question}]\n${s.body}`).join('\n\n')
      setResumeForm(prev => ({
        ...prev,
        title: prev.title || (d.personal?.name ? `${d.personal.name} 이력서` : prev.title),
        summary: coverText || prev.summary,
        skills: skillsCsv || prev.skills,
        targetJob: prev.targetJob || d.targetJob || '',
        resumeData: JSON.stringify(d),
        template: draft.template || template,
      }))
      setHonestyFixes(draft.honestyReport?.fixes || [])
    } catch {
      alert(t('technical.errResumeGenerate'))
    } finally {
      setResumeGenerating(false)
    }
  }

  const handleDownloadPdf = async (id, title) => {
    try {
      const r = await downloadResumePdf(id)
      const url = URL.createObjectURL(new Blob([r], { type: 'application/pdf' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `${title}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      alert(t('technical.errPdfDownload'))
    }
  }

  const openNewCL = () => {
    setEditingCL(null)
    setClForm({ title: '', companyName: '', jobTitle: '', content: '' })
    setClSections([])
    setClCustomSection('')
    setShowCLModal(true)
  }

  const openEditCL = (cl) => {
    setEditingCL(cl)
    setClForm({ title: cl.title, companyName: cl.companyName, jobTitle: cl.jobTitle, content: cl.content })
    setClSections([])
    setClCustomSection('')
    setSelectedCL(null)
    setShowCLModal(true)
  }

  const toggleSection = (s) =>
    setClSections(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s])

  const addCustomSection = () => {
    const v = clCustomSection.trim()
    if (v && !clSections.includes(v)) setClSections(prev => [...prev, v])
    setClCustomSection('')
  }

  const handleSaveCL = async (e) => {
    e.preventDefault()
    try {
      if (editingCL) {
        await updateCoverLetter(editingCL.id, clForm)
      } else {
        await saveCoverLetter(clForm)
      }
      setShowCLModal(false)
      loadData()
    } catch {
      alert(t('technical.errSave'))
    }
  }

  const handleDeleteCL = async (id) => {
    if (!confirm(t('technical.confirmDelete'))) return
    await deleteCoverLetter(id)
    setCoverLetters(prev => prev.filter(c => c.id !== id))
    setSelectedCL(null)
  }

  const handleAiGenerateCL = async () => {
    if (!clForm.companyName.trim() || !clForm.jobTitle.trim()) {
      alert(t('technical.errCompanyJobRequired'))
      return
    }
    setClAiLoading(true)
    try {
      const res = await generateCoverLetter({
        companyName: clForm.companyName.trim(),
        jobTitle: clForm.jobTitle.trim(),
        portfolioIds: portfolios.slice(0, 3).map(p => p.id),
        sections: clSections.length > 0 ? clSections : null,
      })
      setClForm(f => ({ ...f, content: res.data.coverLetter }))
    } catch {
      alert(t('technical.errCoverLetterGenerate'))
    } finally {
      setClAiLoading(false)
    }
  }

  return (
    <Layout title={t('technical.title')}>
      <div className="mb-6 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('technical.title')}</h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('technical.subtitle')}</p>
        </div>
        <div className="flex gap-2">
          {tab === 'portfolio' && (
            <button
              onClick={() => { setShowGenModal(true); setDraft(null); setGenError('') }}
              className="btn-primary text-sm"
            >
              <span className="material-symbols-outlined text-[18px]">auto_awesome</span>{t('technical.aiGenerate')}
            </button>
          )}
          {tab === 'resume' && (
            <button onClick={() => setShowResumeForm(true)} className="btn-primary text-sm">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('technical.createResume')}
            </button>
          )}
          {tab === 'coverletter' && (
            <button onClick={openNewCL} className="btn-primary text-sm">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('technical.createCoverLetter')}
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {[
          { key: 'portfolio',   label: t('technical.tab_portfolio'), icon: 'work_history' },
          { key: 'resume',      label: t('technical.tab_resume'),    icon: 'description'  },
          { key: 'coverletter', label: t('technical.tab_coverLetter'), icon: 'edit_note'    },
        ].map(t => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
              tab === t.key
                ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm'
                : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">{t.icon}</span>{t.label}
          </button>
        ))}
      </div>

      {loading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1,2,3].map(i => <div key={i} className="card p-6 animate-pulse h-40"><div className="h-4 bg-surface-container dark:bg-slate-700 rounded w-2/3 mb-3"/><div className="h-3 bg-surface-container dark:bg-slate-700 rounded w-full mb-2"/><div className="h-3 bg-surface-container dark:bg-slate-700 rounded w-4/5"/></div>)}
        </div>
      )}

      {/* GitHub token banner */}
      {tab === 'portfolio' && (
        <div className={`flex items-center gap-3 p-3 rounded-xl mb-4 text-sm ${
          hasGhToken
            ? 'bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800'
            : 'bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700'
        }`}>
          <span className={`material-symbols-outlined text-[20px] ${hasGhToken ? 'text-green-600 dark:text-green-400' : 'text-outline dark:text-slate-400'}`}>
            {hasGhToken ? 'lock_open' : 'lock'}
          </span>
          <span className={hasGhToken ? 'text-green-700 dark:text-green-300' : 'text-on-surface-variant dark:text-slate-400'}>
            {hasGhToken ? t('technical.ghTokenRegistered') : t('technical.ghTokenNotRegistered')}
          </span>
          <div className="ml-auto flex gap-2">
            <button onClick={() => { setTokenInput(''); setShowTokenModal(true) }}
              className="text-xs px-3 py-1 rounded-lg bg-primary/10 dark:bg-primary-container/20 text-primary dark:text-secondary-fixed font-semibold hover:bg-primary/20 transition-colors">
              {hasGhToken ? t('technical.change') : t('technical.register')}
            </button>
            {hasGhToken && (
              <button onClick={handleDeleteToken}
                className="text-xs px-3 py-1 rounded-lg bg-error/10 dark:bg-error/20 text-error font-semibold hover:bg-error/20 transition-colors">
                {t('technical.delete')}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Portfolio list */}
      {!loading && tab === 'portfolio' && (
        portfolios.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {portfolios.map(p => <PortfolioCard key={p.id} p={p} onDelete={handleDeletePortfolio} onClick={() => setSelectedPortfolio(p)} />)}
          </div>
        ) : (
          <div className="card p-16 text-center">
            <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">work_outline</span>
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('technical.noPortfolio')}</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">{t('technical.noPortfolioDesc')}</p>
            <button onClick={() => { setShowGenModal(true); setDraft(null) }} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">auto_awesome</span>{t('technical.aiGenerate')}
            </button>
          </div>
        )
      )}

      {/* Resume list */}
      {!loading && tab === 'resume' && (
        resumes.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {resumes.map(r => (
              <div key={r.id} className="card p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h4 className="font-bold text-primary dark:text-white">{r.title}</h4>
                    <p className="text-label-md text-outline dark:text-slate-400">{r.targetJob}</p>
                  </div>
                </div>
                {r.summary && <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-3 line-clamp-2">{r.summary}</p>}
                <div className="flex flex-wrap gap-1.5 mb-4">
                  {r.skills?.slice(0,4).map(s => (
                    <span key={s} className="text-[10px] px-2 py-0.5 bg-secondary-container/30 dark:bg-secondary-fixed/10 text-on-secondary-container dark:text-secondary-fixed rounded-full">{s}</span>
                  ))}
                </div>
                <p className="text-label-md text-outline dark:text-slate-400 mb-3">{t('technical.linkedProjects', { count: r.portfolios?.length ?? 0 })}</p>
                <div className="flex gap-2">
                  <button onClick={() => handleDownloadPdf(r.id, r.title)} className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold flex items-center justify-center gap-1.5 hover:scale-[1.02] active:scale-95 transition-transform">
                    <span className="material-symbols-outlined text-[16px]">download</span>PDF
                  </button>
                  <button onClick={() => openEditResume(r.id)} className="py-2.5 px-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl hover:bg-surface-container dark:hover:bg-slate-800 transition-colors" title={t('technical.edit')}>
                    <span className="material-symbols-outlined text-[16px]">edit</span>
                  </button>
                  <button onClick={async () => { await deleteResume(r.id); loadData() }} className="py-2.5 px-3 border border-error/30 text-error rounded-xl hover:bg-error-container dark:hover:bg-error/20 transition-colors">
                    <span className="material-symbols-outlined text-[16px]">delete</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="card p-16 text-center">
            <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">description</span>
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('technical.noResume')}</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">{t('technical.noResumeDesc')}</p>
            <button onClick={() => setShowResumeForm(true)} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('technical.createResume')}
            </button>
          </div>
        )
      )}

      {/* ── 자기소개서 탭 콘텐츠 ── */}
      {!loading && tab === 'coverletter' && (
        coverLetters.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {coverLetters.map(cl => (
              <div
                key={cl.id}
                onClick={() => setSelectedCL(cl)}
                className="card p-5 group cursor-pointer hover:shadow-md hover:border-primary/30 dark:hover:border-secondary-fixed/30 transition-all active:scale-[0.98] relative"
              >
                {/* 회사·직무 뱃지 */}
                <div className="flex items-center gap-2 mb-3 flex-wrap">
                  <span className="text-[10px] font-bold px-2.5 py-1 bg-primary/10 dark:bg-primary-container/30 text-primary dark:text-secondary-fixed rounded-full">
                    {cl.companyName}
                  </span>
                  <span className="text-[10px] text-outline dark:text-slate-400">{cl.jobTitle}</span>
                </div>
                <h4 className="font-bold text-primary dark:text-white mb-2 group-hover:text-primary dark:group-hover:text-secondary-fixed transition-colors">
                  {cl.title}
                </h4>
                <p className="text-sm text-on-surface-variant dark:text-slate-400 line-clamp-3 leading-relaxed">
                  {cl.preview}
                </p>
                <p className="text-label-md text-outline dark:text-slate-500 mt-3">
                  {new Date(cl.updatedAt).toLocaleDateString('ko-KR')}
                </p>
                <button
                  onClick={e => { e.stopPropagation(); handleDeleteCL(cl.id) }}
                  className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 p-1.5 rounded-lg bg-error-container dark:bg-error/20 text-error transition-all"
                >
                  <span className="material-symbols-outlined text-[16px]">delete</span>
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="card p-16 text-center">
            <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">edit_note</span>
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('technical.noCoverLetter')}</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">{t('technical.noCoverLetterDesc')}</p>
            <button onClick={openNewCL} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">add</span>{t('technical.createCoverLetter')}
            </button>
          </div>
        )
      )}

      {/* ── 자기소개서 상세 모달 ── */}
      {selectedCL && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => { if (e.target === e.currentTarget) setSelectedCL(null) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-2xl shadow-2xl max-h-[90vh] flex flex-col">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between sticky top-0 bg-white dark:bg-slate-900">
              <div>
                <div className="flex items-center gap-2 mb-1 flex-wrap">
                  <span className="text-[11px] font-bold px-2.5 py-1 bg-primary/10 dark:bg-primary-container/30 text-primary dark:text-secondary-fixed rounded-full">{selectedCL.companyName}</span>
                  <span className="text-label-md text-outline dark:text-slate-400">{selectedCL.jobTitle}</span>
                </div>
                <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{selectedCL.title}</h3>
              </div>
              <div className="flex items-center gap-2 shrink-0 ml-3">
                <button onClick={() => openEditCL(selectedCL)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400" title={t('technical.edit')}>
                  <span className="material-symbols-outlined text-[20px]">edit</span>
                </button>
                <button onClick={() => setSelectedCL(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400">
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>
            </div>
            <div className="p-6 overflow-y-auto flex-1">
              <p className="text-sm text-on-surface dark:text-slate-300 leading-relaxed whitespace-pre-wrap">{selectedCL.content}</p>
            </div>
            <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex gap-2">
              <button onClick={() => {
                navigator.clipboard.writeText(selectedCL.content)
                alert(t('technical.copiedToClipboard'))
              }} className="flex-1 py-2.5 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-[18px]">content_copy</span>{t('technical.copy')}
              </button>
              <button onClick={() => handleDeleteCL(selectedCL.id)}
                className="py-2.5 px-4 border border-error/30 text-error rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-error-container dark:hover:bg-error/20 transition-colors">
                <span className="material-symbols-outlined text-[18px]">delete</span>{t('technical.delete')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── 자기소개서 생성/수정 모달 ── */}
      {showCLModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => { if (e.target === e.currentTarget) setShowCLModal(false) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">edit_note</span>
                {editingCL ? t('technical.editCoverLetter') : t('technical.createCoverLetter')}
              </h3>
              <button onClick={() => setShowCLModal(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSaveCL} className="p-6 space-y-4">
              {/* 제목 */}
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldTitle')}</label>
                <input value={clForm.title} onChange={e => setClForm(f => ({...f, title: e.target.value}))}
                  placeholder={t('technical.clTitlePlaceholder')} required
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>

              {/* 회사 + 직무 */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.targetCompany')}</label>
                  <input value={clForm.companyName} onChange={e => setClForm(f => ({...f, companyName: e.target.value}))}
                    placeholder={t('technical.companyPlaceholder')} required
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.jobTitle')}</label>
                  <input value={clForm.jobTitle} onChange={e => setClForm(f => ({...f, jobTitle: e.target.value}))}
                    placeholder={t('technical.jobPlaceholder')} required
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
              </div>

              {/* 소제목 선택 */}
              <div>
                <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-2">
                  {t('technical.sectionLabel')} <span className="text-outline dark:text-slate-500">{t('technical.sectionHint')}</span>
                </p>
                {/* 프리셋 */}
                <div className="flex flex-wrap gap-2 mb-2">
                  {[
                    { code: 'motivation', value: '지원동기' },
                    { code: 'growth', value: '성장과정' },
                    { code: 'competency', value: '직무역량' },
                    { code: 'aspiration', value: '입사 후 포부' },
                    { code: 'teamwork', value: '팀워크/협업경험' },
                    { code: 'problemSolving', value: '문제해결 경험' },
                  ].map(({ code, value }) => (
                    <button key={code} type="button" onClick={() => toggleSection(value)}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
                        clSections.includes(value)
                          ? 'bg-primary dark:bg-primary-container text-white'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>
                      {clSections.includes(value) && <span className="mr-1">✓</span>}{t(`technical.section_${code}`)}
                    </button>
                  ))}
                </div>
                {/* 직접 입력 */}
                <div className="flex gap-2">
                  <input value={clCustomSection} onChange={e => setClCustomSection(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addCustomSection())}
                    placeholder={t('technical.customSectionPlaceholder')}
                    className="flex-1 px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  <button type="button" onClick={addCustomSection}
                    className="px-3 py-2 bg-surface-container dark:bg-slate-700 text-primary dark:text-secondary-fixed rounded-xl text-sm font-bold hover:bg-surface-container-high dark:hover:bg-slate-600 transition-colors">
                    {t('technical.add')}
                  </button>
                </div>
                {/* 선택된 소제목 */}
                {clSections.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {clSections.map((s, i) => (
                      <span key={s} className="flex items-center gap-1 px-2.5 py-1 bg-secondary-container/20 dark:bg-secondary-fixed/10 text-on-secondary-container dark:text-secondary-fixed rounded-full text-xs font-bold border border-secondary-fixed/20">
                        {i + 1}. {s}
                        <button type="button" onClick={() => setClSections(prev => prev.filter(x => x !== s))}
                          className="ml-0.5 hover:text-error transition-colors">×</button>
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* AI 생성 버튼 */}
              <button type="button" disabled={clAiLoading || !clForm.companyName || !clForm.jobTitle}
                onClick={handleAiGenerateCL}
                className="w-full py-2.5 bg-primary/10 dark:bg-primary-container/20 text-primary dark:text-secondary-fixed border border-primary/20 dark:border-primary-container/40 rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:bg-primary/20 dark:hover:bg-primary-container/30 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                {clAiLoading
                  ? <><div className="w-4 h-4 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />{t('technical.aiWriting')}</>
                  : <><span className="material-symbols-outlined text-[18px]">auto_awesome</span>{t('technical.aiWriteCoverLetter')}</>
                }
              </button>

              {/* 내용 */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('technical.content')}</label>
                  <span className="text-label-md text-outline dark:text-slate-500">{t('technical.charCount', { count: clForm.content.length })}</span>
                </div>
                <textarea value={clForm.content} onChange={e => setClForm(f => ({...f, content: e.target.value}))}
                  placeholder={t('technical.contentPlaceholder')} rows={10} required
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>

              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowCLModal(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  {t('technical.cancel')}
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {editingCL ? t('technical.editComplete') : t('technical.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── AI Generate Modal ── */}
      {showGenModal && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => { if (e.target === e.currentTarget) { setShowGenModal(false); setDraft(null) }}}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">auto_awesome</span>{t('technical.aiPortfolioGenerate')}
              </h3>
              <button onClick={() => { setShowGenModal(false); setDraft(null) }} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>

            <div className="p-6 space-y-5">
              {!draft ? (
                <>
                  {/* GitHub input */}
                  <div>
                    <p className="font-bold text-sm text-primary dark:text-white mb-2">{t('technical.githubUrl')}</p>
                    <div className="flex gap-2">
                      <input
                        value={githubUrl}
                        onChange={e => setGithubUrl(e.target.value)}
                        placeholder="https://github.com/username/repo"
                        className="flex-1 px-4 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                      />
                      <button
                        onClick={handleGithubGenerate}
                        disabled={genLoading || !githubUrl.trim()}
                        className="px-4 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50 hover:scale-[1.02] active:scale-95 transition-transform"
                      >
                        {genLoading ? t('technical.analyzingShort') : t('technical.analyze')}
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="flex-1 h-px bg-outline-variant dark:bg-slate-700" />
                    <span className="text-label-md text-outline dark:text-slate-500">{t('technical.or')}</span>
                    <div className="flex-1 h-px bg-outline-variant dark:bg-slate-700" />
                  </div>

                  {/* File upload */}
                  <div>
                    <p className="font-bold text-sm text-primary dark:text-white mb-2">{t('technical.fileUpload')}</p>
                    <input type="file" accept=".pdf,.pptx" ref={fileRef} className="hidden" onChange={e => e.target.files[0] && handleFileGenerate(e.target.files[0])} />
                    <button
                      onClick={() => fileRef.current?.click()}
                      disabled={genLoading}
                      className="w-full py-8 border-2 border-dashed border-outline-variant dark:border-slate-700 rounded-2xl text-on-surface-variant dark:text-slate-400 hover:border-primary dark:hover:border-secondary-fixed hover:text-primary dark:hover:text-secondary-fixed transition-colors disabled:opacity-50 flex flex-col items-center gap-2"
                    >
                      <span className="material-symbols-outlined text-3xl">{genLoading ? 'hourglass_empty' : 'upload_file'}</span>
                      <span className="text-sm font-medium">{genLoading ? t('technical.analyzing') : t('technical.fileUploadHint')}</span>
                    </button>
                  </div>

                  {genError && <p className="text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{genError}</p>}
                </>
              ) : (
                /* Draft preview */
                <>
                  <div className="p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30 mb-2">
                    <p className="text-label-md text-outline dark:text-slate-400 mb-1">{t('technical.draftNotice')}</p>
                  </div>
                  {[
                    { label: t('technical.fieldTitle'), val: draft.title },
                    { label: t('technical.fieldRole'), val: draft.role },
                    { label: t('technical.fieldDescription'), val: draft.description },
                    { label: t('technical.techStack'), val: draft.techStack?.join(', ') },
                    { label: t('technical.fieldStatus'), val: draft.status === 'COMPLETED' ? t('technical.status_completed') : t('technical.status_inProgress') },
                  ].map(f => f.val && (
                    <div key={f.label} className="space-y-1">
                      <p className="text-label-md text-outline dark:text-slate-400">{f.label}</p>
                      <p className="text-sm text-on-surface dark:text-white bg-surface-container-low dark:bg-slate-800 px-3 py-2 rounded-lg">{f.val}</p>
                    </div>
                  ))}
                  {genError && <p className="text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{genError}</p>}
                  <div className="flex gap-3 pt-2">
                    <button onClick={() => setDraft(null)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                      {t('technical.regenerate')}
                    </button>
                    <button onClick={handleSaveDraft} className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                      {t('technical.saveDraft')}
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Resume create modal ── */}
      {showResumeForm && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => { if (e.target === e.currentTarget) setShowResumeForm(false) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">description</span>{t('technical.createResume')}
              </h3>
              <button onClick={() => setShowResumeForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreateResume} className="p-6 space-y-4">
              <div className="flex items-center gap-2 flex-wrap">
                <button
                  type="button"
                  className="btn-secondary text-sm"
                  disabled={resumeGenerating}
                  onClick={() => handleGenerateResume('general')}
                >
                  <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                  {resumeGenerating ? t('technical.generatingResume') : t('technical.aiGenerateResume')}
                </button>
                {honestyFixes.length > 0 && (
                  <span
                    className="chip"
                    title={honestyFixes.map(f => f.reason).filter(Boolean).join('\n')}
                  >
                    {t('technical.honestyFixed')} {honestyFixes.length}
                  </span>
                )}
              </div>
              {[
                { key: 'title', label: t('technical.resumeTitle'), placeholder: t('technical.resumeTitlePlaceholder') },
                { key: 'targetJob', label: t('technical.desiredJob'), placeholder: t('technical.jobPlaceholder') },
                { key: 'skills', label: t('technical.skillsLabel'), placeholder: t('technical.skillsPlaceholder') },
              ].map(f => (
                <div key={f.key}>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{f.label}</label>
                  <input
                    value={resumeForm[f.key]}
                    onChange={e => setResumeForm(prev => ({ ...prev, [f.key]: e.target.value }))}
                    placeholder={f.placeholder}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                    required={f.key === 'title'}
                  />
                </div>
              ))}
              {/* AI 자기소개서 생성 */}
              <div className="p-4 bg-primary/5 dark:bg-primary-container/20 rounded-2xl border border-primary/20 dark:border-primary-container/40 space-y-3">
                <p className="font-bold text-sm text-primary dark:text-white flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                  {t('technical.aiCoverLetterAuto')}
                </p>
                <p className="text-label-md text-on-surface-variant dark:text-slate-400">
                  {t('technical.aiCoverLetterAutoDesc')}
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    value={coverCompany}
                    onChange={e => setCoverCompany(e.target.value)}
                    placeholder={t('technical.coverCompanyPlaceholder')}
                    className="px-3 py-2.5 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                  <input
                    value={coverJobTitle}
                    onChange={e => setCoverJobTitle(e.target.value)}
                    placeholder={t('technical.coverJobPlaceholder')}
                    className="px-3 py-2.5 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                </div>
                <button
                  type="button"
                  disabled={coverLoading || !coverCompany.trim() || !coverJobTitle.trim()}
                  onClick={async () => {
                    setCoverLoading(true)
                    try {
                      const res = await generateCoverLetter({
                        companyName: coverCompany.trim(),
                        jobTitle: coverJobTitle.trim(),
                        portfolioIds: portfolios.slice(0, 3).map(p => p.id),
                      })
                      setResumeForm(prev => ({ ...prev, summary: res.data.coverLetter }))
                    } catch {
                      alert(t('technical.errCoverLetterGenerate'))
                    } finally {
                      setCoverLoading(false)
                    }
                  }}
                  className="w-full py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {coverLoading
                    ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />{t('technical.aiWriting')}</>
                    : <><span className="material-symbols-outlined text-[18px]">edit_note</span>{t('technical.createCoverLetter')}</>
                  }
                </button>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('technical.tab_coverLetter')}</label>
                  {resumeForm.summary && (
                    <span className="text-label-md text-outline dark:text-slate-500">{t('technical.charCount', { count: resumeForm.summary.length })}</span>
                  )}
                </div>
                <textarea
                  value={resumeForm.summary}
                  onChange={e => setResumeForm(prev => ({ ...prev, summary: e.target.value }))}
                  placeholder={t('technical.contentPlaceholder')}
                  rows={6}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none"
                />
              </div>
              {portfolios.length > 0 && (
                <div>
                  <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-2">{t('technical.linkPortfolios', { count: portfolios.length })}</p>
                  <div className="space-y-2 max-h-32 overflow-y-auto">
                    {portfolios.slice(0, 3).map(p => (
                      <div key={p.id} className="flex items-center gap-2 text-sm text-on-surface dark:text-slate-300 bg-surface-container-low dark:bg-slate-800 px-3 py-2 rounded-lg">
                        <span className="material-symbols-outlined text-[14px] text-secondary dark:text-secondary-fixed">check_circle</span>{p.title}
                      </div>
                    ))}
                  </div>
                </div>
              )}
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowResumeForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  {t('technical.cancel')}
                </button>
                <button type="submit" className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {t('technical.create')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 포트폴리오 상세 모달 */}
      {selectedPortfolio && (
        <PortfolioDetailModal
          p={selectedPortfolio}
          onClose={() => setSelectedPortfolio(null)}
          onDelete={(id) => { handleDeletePortfolio(id); setSelectedPortfolio(null) }}
          onEdit={openEditPortfolio}
        />
      )}

      {/* 포트폴리오 수정 모달 */}
      {editingPortfolio && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => { if (e.target === e.currentTarget) setEditingPortfolio(null) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">edit</span>{t('technical.editPortfolio')}
              </h3>
              <button onClick={() => setEditingPortfolio(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSavePortfolioEdit} className="p-6 space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldTitleRequired')}</label>
                <input required value={editingPortfolio.title} onChange={e => setEditingPortfolio(p => ({...p, title: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldRoleRequired')}</label>
                <input required value={editingPortfolio.role} onChange={e => setEditingPortfolio(p => ({...p, role: e.target.value}))}
                  placeholder={t('technical.rolePlaceholder')}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldDescription')}</label>
                <textarea value={editingPortfolio.description} onChange={e => setEditingPortfolio(p => ({...p, description: e.target.value}))}
                  rows={4}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.techStackLabel')}</label>
                <input value={editingPortfolio.techStack} onChange={e => setEditingPortfolio(p => ({...p, techStack: e.target.value}))}
                  placeholder={t('technical.skillsPlaceholder')}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.startDate')}</label>
                  <input type="date" value={editingPortfolio.startDate} onChange={e => setEditingPortfolio(p => ({...p, startDate: e.target.value}))}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.endDate')}</label>
                  <input type="date" value={editingPortfolio.endDate} onChange={e => setEditingPortfolio(p => ({...p, endDate: e.target.value}))}
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">GitHub URL</label>
                <input type="url" value={editingPortfolio.githubUrl} onChange={e => setEditingPortfolio(p => ({...p, githubUrl: e.target.value}))}
                  placeholder="https://github.com/..."
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.deployUrl')}</label>
                <input type="url" value={editingPortfolio.deployUrl} onChange={e => setEditingPortfolio(p => ({...p, deployUrl: e.target.value}))}
                  placeholder="https://..."
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldStatus')}</label>
                <div className="flex gap-2">
                  {['IN_PROGRESS', 'COMPLETED'].map(s => (
                    <button key={s} type="button" onClick={() => setEditingPortfolio(p => ({...p, status: s}))}
                      className={`flex-1 py-2.5 rounded-xl text-sm font-bold transition-all ${
                        editingPortfolio.status === s
                          ? 'bg-primary dark:bg-primary-container text-white'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300'
                      }`}>
                      {t(`technical.${STATUS_LABEL[s]}`)}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setEditingPortfolio(null)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  {t('technical.cancel')}
                </button>
                <button type="submit" disabled={portfolioEditSaving}
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50">
                  {portfolioEditSaving ? t('technical.saving') : t('technical.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 이력서 수정 모달 */}
      {editingResume && (
        <div className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4" onClick={e => { if (e.target === e.currentTarget) setEditingResume(null) }}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">edit</span>{t('technical.editResume')}
              </h3>
              <button onClick={() => setEditingResume(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSaveResumeEdit} className="p-6 space-y-4">
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.fieldTitleRequired')}</label>
                <input required value={editingResume.title} onChange={e => setEditingResume(r => ({...r, title: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.desiredJob')}</label>
                <input value={editingResume.targetJob} onChange={e => setEditingResume(r => ({...r, targetJob: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.skillsLabel')}</label>
                <input value={editingResume.skills} onChange={e => setEditingResume(r => ({...r, skills: e.target.value}))}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('technical.selfIntro')}</label>
                <textarea value={editingResume.summary} onChange={e => setEditingResume(r => ({...r, summary: e.target.value}))}
                  rows={8}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setEditingResume(null)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  {t('technical.cancel')}
                </button>
                <button type="submit" disabled={resumeEditSaving}
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50">
                  {resumeEditSaving ? t('technical.saving') : t('technical.save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* GitHub 토큰 설정 모달 */}
      {showTokenModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => setShowTokenModal(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
            <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-1">{t('technical.ghTokenTitle')}</h3>
            <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-4">
              {t('technical.ghTokenDesc1')}<br />
              {t('technical.ghTokenDesc2')}<br />
              <span className="text-xs text-outline dark:text-slate-500">{t('technical.ghTokenPermission')} <code>Contents (read)</code></span>
            </p>
            <input
              value={tokenInput}
              onChange={e => setTokenInput(e.target.value)}
              placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
              type="password"
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary/30 mb-4"
            />
            <div className="flex gap-3">
              <button onClick={() => setShowTokenModal(false)}
                className="flex-1 py-2.5 border border-outline-variant dark:border-slate-700 dark:text-slate-300 rounded-xl text-sm font-semibold hover:bg-surface-container transition-colors">
                {t('technical.cancel')}
              </button>
              <button onClick={handleSaveToken} disabled={!tokenInput.trim() || tokenSaving}
                className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.01] active:scale-95 transition-transform disabled:opacity-50">
                {tokenSaving ? t('technical.saving') : t('technical.save')}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
