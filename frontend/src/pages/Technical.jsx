import { useState, useEffect, useRef } from 'react'
import Layout from '../components/layout/Layout'
import {
  getPortfolios, createPortfolio, deletePortfolio,
  generateFromGithub, generateFromFile
} from '../api/portfolio'
import { getResumes, createResume, downloadResumePdf, deleteResume } from '../api/resume'
import { generateCoverLetter } from '../api/assistant'
import { getCoverLetters, saveCoverLetter, updateCoverLetter, deleteCoverLetter } from '../api/coverLetter'
import { getGithubTokenStatus, saveGithubToken, deleteGithubToken } from '../api/career'

const STATUS_LABEL = { IN_PROGRESS: '진행 중', COMPLETED: '완료' }
const STATUS_COLOR = {
  IN_PROGRESS: 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed',
  COMPLETED:   'bg-surface-container dark:bg-slate-700 text-on-surface-variant dark:text-slate-300',
}

function PortfolioCard({ p, onDelete, onClick }) {
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
          {STATUS_LABEL[p.status]}
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
        {p.startDate && <span>{p.startDate} ~ {p.endDate ?? '현재'}</span>}
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

function PortfolioDetailModal({ p, onClose, onDelete }) {
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
              {STATUS_LABEL[p.status]}
            </span>
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
              {p.startDate} ~ {p.endDate ?? '진행 중'}
            </div>
          )}

          {/* 설명 */}
          {p.description && (
            <div>
              <p className="text-label-md text-outline dark:text-slate-500 mb-2">프로젝트 설명</p>
              <p className="text-sm text-on-surface dark:text-slate-300 leading-relaxed bg-surface-container-low dark:bg-slate-800 p-4 rounded-xl">
                {p.description}
              </p>
            </div>
          )}

          {/* 기술 스택 */}
          {p.techStack?.length > 0 && (
            <div>
              <p className="text-label-md text-outline dark:text-slate-500 mb-2">기술 스택</p>
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
            <span className="material-symbols-outlined text-[18px]">delete</span>포트폴리오 삭제
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Technical() {
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

  // GitHub token state
  const [hasGhToken, setHasGhToken] = useState(false)
  const [showTokenModal, setShowTokenModal] = useState(false)
  const [tokenInput, setTokenInput] = useState('')
  const [tokenSaving, setTokenSaving] = useState(false)

  // Resume create state
  const [showResumeForm, setShowResumeForm] = useState(false)
  const [resumeForm, setResumeForm] = useState({ title: '', summary: '', skills: '', targetJob: '', portfolioIds: [] })

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
    getGithubTokenStatus().then(r => setHasGhToken(r.data?.data?.hasToken ?? false)).catch(() => {})
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
      setGenError('GitHub에서 정보를 가져오지 못했습니다.')
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
      setGenError('파일 분석 중 오류가 발생했습니다.')
    } finally {
      setGenLoading(false)
    }
  }

  const handleSaveDraft = async () => {
    if (!draft) return
    if (!draft.title?.trim()) { setGenError('제목이 비어있습니다. AI가 분석에 실패했을 수 있습니다.'); return }
    if (!draft.role?.trim()) { setGenError('역할이 비어있습니다. 직접 입력 후 저장해 주세요.'); return }
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
      setGenError(err?.message || '저장 중 오류가 발생했습니다.')
    }
  }

  const handleDeletePortfolio = async (id) => {
    if (!confirm('포트폴리오를 삭제하시겠습니까?')) return
    await deletePortfolio(id)
    setPortfolios(prev => prev.filter(p => p.id !== id))
  }

  const handleCreateResume = async (e) => {
    e.preventDefault()
    try {
      await createResume({ ...resumeForm, portfolioIds: portfolios.slice(0, 3).map(p => p.id) })
      setShowResumeForm(false)
      setResumeForm({ title: '', summary: '', skills: '', targetJob: '', portfolioIds: [] })
      if (tab === 'resume') loadData()
    } catch {
      alert('이력서 생성 중 오류가 발생했습니다.')
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
      alert('PDF 다운로드 중 오류가 발생했습니다.')
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
      alert('저장 중 오류가 발생했습니다.')
    }
  }

  const handleDeleteCL = async (id) => {
    if (!confirm('삭제하시겠습니까?')) return
    await deleteCoverLetter(id)
    setCoverLetters(prev => prev.filter(c => c.id !== id))
    setSelectedCL(null)
  }

  const handleAiGenerateCL = async () => {
    if (!clForm.companyName.trim() || !clForm.jobTitle.trim()) {
      alert('회사명과 직무를 먼저 입력해주세요.')
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
      alert('자기소개서 생성 중 오류가 발생했습니다.')
    } finally {
      setClAiLoading(false)
    }
  }

  return (
    <Layout title="포트폴리오 · 이력서">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">포트폴리오 · 이력서</h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">프로젝트를 등록하고 이력서를 자동으로 완성하세요.</p>
        </div>
        <div className="flex gap-2">
          {tab === 'portfolio' && (
            <button
              onClick={() => { setShowGenModal(true); setDraft(null); setGenError('') }}
              className="btn-primary text-sm"
            >
              <span className="material-symbols-outlined text-[18px]">auto_awesome</span>AI로 만들기
            </button>
          )}
          {tab === 'resume' && (
            <button onClick={() => setShowResumeForm(true)} className="btn-primary text-sm">
              <span className="material-symbols-outlined text-[18px]">add</span>이력서 생성
            </button>
          )}
          {tab === 'coverletter' && (
            <button onClick={openNewCL} className="btn-primary text-sm">
              <span className="material-symbols-outlined text-[18px]">add</span>자기소개서 생성
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {[
          { key: 'portfolio',   label: '포트폴리오', icon: 'work_history' },
          { key: 'resume',      label: '이력서',     icon: 'description'  },
          { key: 'coverletter', label: '자기소개서', icon: 'edit_note'    },
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
            {hasGhToken ? 'GitHub 개인 토큰 등록됨 — 프라이빗 저장소 접근 가능' : 'GitHub 개인 토큰 미등록 — 퍼블릭 저장소만 분석 가능'}
          </span>
          <div className="ml-auto flex gap-2">
            <button onClick={() => { setTokenInput(''); setShowTokenModal(true) }}
              className="text-xs px-3 py-1 rounded-lg bg-primary/10 dark:bg-primary-container/20 text-primary dark:text-secondary-fixed font-semibold hover:bg-primary/20 transition-colors">
              {hasGhToken ? '변경' : '등록'}
            </button>
            {hasGhToken && (
              <button onClick={handleDeleteToken}
                className="text-xs px-3 py-1 rounded-lg bg-error/10 dark:bg-error/20 text-error font-semibold hover:bg-error/20 transition-colors">
                삭제
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
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">포트폴리오가 없습니다</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">GitHub 링크나 PPT 파일로 AI가 자동으로 만들어드립니다.</p>
            <button onClick={() => { setShowGenModal(true); setDraft(null) }} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">auto_awesome</span>AI로 만들기
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
                <p className="text-label-md text-outline dark:text-slate-400 mb-3">연결된 프로젝트 {r.portfolios?.length ?? 0}개</p>
                <div className="flex gap-2">
                  <button onClick={() => handleDownloadPdf(r.id, r.title)} className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold flex items-center justify-center gap-1.5 hover:scale-[1.02] active:scale-95 transition-transform">
                    <span className="material-symbols-outlined text-[16px]">download</span>PDF
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
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">이력서가 없습니다</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">포트폴리오를 연결해 이력서를 자동으로 완성하세요.</p>
            <button onClick={() => setShowResumeForm(true)} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">add</span>이력서 생성
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
            <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">자기소개서가 없습니다</p>
            <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2 mb-6">지원 회사에 맞는 자기소개서를 AI가 작성해드립니다.</p>
            <button onClick={openNewCL} className="btn-primary mx-auto">
              <span className="material-symbols-outlined text-[18px]">add</span>자기소개서 생성
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
                <button onClick={() => openEditCL(selectedCL)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400" title="수정">
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
                alert('클립보드에 복사됐습니다.')
              }} className="flex-1 py-2.5 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-[18px]">content_copy</span>복사
              </button>
              <button onClick={() => handleDeleteCL(selectedCL.id)}
                className="py-2.5 px-4 border border-error/30 text-error rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-error-container dark:hover:bg-error/20 transition-colors">
                <span className="material-symbols-outlined text-[18px]">delete</span>삭제
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
                {editingCL ? '자기소개서 수정' : '자기소개서 생성'}
              </h3>
              <button onClick={() => setShowCLModal(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleSaveCL} className="p-6 space-y-4">
              {/* 제목 */}
              <div>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">제목</label>
                <input value={clForm.title} onChange={e => setClForm(f => ({...f, title: e.target.value}))}
                  placeholder="예: 카카오 백엔드 지원" required
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>

              {/* 회사 + 직무 */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">지원 회사</label>
                  <input value={clForm.companyName} onChange={e => setClForm(f => ({...f, companyName: e.target.value}))}
                    placeholder="예: 카카오" required
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
                <div>
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">직무</label>
                  <input value={clForm.jobTitle} onChange={e => setClForm(f => ({...f, jobTitle: e.target.value}))}
                    placeholder="예: 백엔드 개발자" required
                    className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                </div>
              </div>

              {/* 소제목 선택 */}
              <div>
                <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-2">
                  소제목 <span className="text-outline dark:text-slate-500">(선택 · AI가 항목별로 작성)</span>
                </p>
                {/* 프리셋 */}
                <div className="flex flex-wrap gap-2 mb-2">
                  {['지원동기', '성장과정', '직무역량', '입사 후 포부', '팀워크/협업경험', '문제해결 경험'].map(s => (
                    <button key={s} type="button" onClick={() => toggleSection(s)}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${
                        clSections.includes(s)
                          ? 'bg-primary dark:bg-primary-container text-white'
                          : 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:bg-surface-container-high dark:hover:bg-slate-700'
                      }`}>
                      {clSections.includes(s) && <span className="mr-1">✓</span>}{s}
                    </button>
                  ))}
                </div>
                {/* 직접 입력 */}
                <div className="flex gap-2">
                  <input value={clCustomSection} onChange={e => setClCustomSection(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addCustomSection())}
                    placeholder="직접 입력 후 Enter"
                    className="flex-1 px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                  <button type="button" onClick={addCustomSection}
                    className="px-3 py-2 bg-surface-container dark:bg-slate-700 text-primary dark:text-secondary-fixed rounded-xl text-sm font-bold hover:bg-surface-container-high dark:hover:bg-slate-600 transition-colors">
                    추가
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
                  ? <><div className="w-4 h-4 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />AI 작성 중...</>
                  : <><span className="material-symbols-outlined text-[18px]">auto_awesome</span>AI로 자기소개서 자동 작성</>
                }
              </button>

              {/* 내용 */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400">내용</label>
                  <span className="text-label-md text-outline dark:text-slate-500">{clForm.content.length}자</span>
                </div>
                <textarea value={clForm.content} onChange={e => setClForm(f => ({...f, content: e.target.value}))}
                  placeholder="AI로 생성하거나 직접 작성해주세요." rows={10} required
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
              </div>

              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowCLModal(false)}
                  className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                  취소
                </button>
                <button type="submit"
                  className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  {editingCL ? '수정 완료' : '저장'}
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
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">auto_awesome</span>AI 포트폴리오 생성
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
                    <p className="font-bold text-sm text-primary dark:text-white mb-2">GitHub 저장소</p>
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
                        {genLoading ? '분석 중...' : '분석'}
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="flex-1 h-px bg-outline-variant dark:bg-slate-700" />
                    <span className="text-label-md text-outline dark:text-slate-500">또는</span>
                    <div className="flex-1 h-px bg-outline-variant dark:bg-slate-700" />
                  </div>

                  {/* File upload */}
                  <div>
                    <p className="font-bold text-sm text-primary dark:text-white mb-2">파일 업로드 (PDF · PPTX)</p>
                    <input type="file" accept=".pdf,.pptx" ref={fileRef} className="hidden" onChange={e => e.target.files[0] && handleFileGenerate(e.target.files[0])} />
                    <button
                      onClick={() => fileRef.current?.click()}
                      disabled={genLoading}
                      className="w-full py-8 border-2 border-dashed border-outline-variant dark:border-slate-700 rounded-2xl text-on-surface-variant dark:text-slate-400 hover:border-primary dark:hover:border-secondary-fixed hover:text-primary dark:hover:text-secondary-fixed transition-colors disabled:opacity-50 flex flex-col items-center gap-2"
                    >
                      <span className="material-symbols-outlined text-3xl">{genLoading ? 'hourglass_empty' : 'upload_file'}</span>
                      <span className="text-sm font-medium">{genLoading ? 'AI 분석 중...' : '파일을 드래그하거나 클릭해 업로드'}</span>
                    </button>
                  </div>

                  {genError && <p className="text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{genError}</p>}
                </>
              ) : (
                /* Draft preview */
                <>
                  <div className="p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30 mb-2">
                    <p className="text-label-md text-outline dark:text-slate-400 mb-1">AI가 생성한 초안입니다. 저장 후 수정할 수 있습니다.</p>
                  </div>
                  {[
                    { label: '제목', val: draft.title },
                    { label: '역할', val: draft.role },
                    { label: '설명', val: draft.description },
                    { label: '기술 스택', val: draft.techStack?.join(', ') },
                    { label: '상태', val: draft.status === 'COMPLETED' ? '완료' : '진행 중' },
                  ].map(f => f.val && (
                    <div key={f.label} className="space-y-1">
                      <p className="text-label-md text-outline dark:text-slate-400">{f.label}</p>
                      <p className="text-sm text-on-surface dark:text-white bg-surface-container-low dark:bg-slate-800 px-3 py-2 rounded-lg">{f.val}</p>
                    </div>
                  ))}
                  {genError && <p className="text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{genError}</p>}
                  <div className="flex gap-3 pt-2">
                    <button onClick={() => setDraft(null)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                      다시 생성
                    </button>
                    <button onClick={handleSaveDraft} className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                      저장하기
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
                <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">description</span>이력서 생성
              </h3>
              <button onClick={() => setShowResumeForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreateResume} className="p-6 space-y-4">
              {[
                { key: 'title', label: '이력서 제목', placeholder: '예: 백엔드 개발자 지원' },
                { key: 'targetJob', label: '희망 직무', placeholder: '예: 백엔드 개발자' },
                { key: 'skills', label: '보유 기술 (쉼표 구분)', placeholder: '예: Java, Spring Boot, MySQL' },
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
                  AI 자기소개서 자동 생성
                </p>
                <p className="text-label-md text-on-surface-variant dark:text-slate-400">
                  지원 회사와 직무를 입력하면 AI가 포트폴리오·성적·수상내역을 분석해 맞춤 자기소개서를 작성합니다.
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    value={coverCompany}
                    onChange={e => setCoverCompany(e.target.value)}
                    placeholder="지원 회사 (예: 카카오)"
                    className="px-3 py-2.5 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                  <input
                    value={coverJobTitle}
                    onChange={e => setCoverJobTitle(e.target.value)}
                    placeholder="직무 (예: 백엔드 개발자)"
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
                      alert('자기소개서 생성 중 오류가 발생했습니다.')
                    } finally {
                      setCoverLoading(false)
                    }
                  }}
                  className="w-full py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {coverLoading
                    ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />AI 작성 중...</>
                    : <><span className="material-symbols-outlined text-[18px]">edit_note</span>자기소개서 생성</>
                  }
                </button>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-label-md text-on-surface-variant dark:text-slate-400">자기소개서</label>
                  {resumeForm.summary && (
                    <span className="text-label-md text-outline dark:text-slate-500">{resumeForm.summary.length}자</span>
                  )}
                </div>
                <textarea
                  value={resumeForm.summary}
                  onChange={e => setResumeForm(prev => ({ ...prev, summary: e.target.value }))}
                  placeholder="AI로 생성하거나 직접 작성해주세요."
                  rows={6}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none"
                />
              </div>
              {portfolios.length > 0 && (
                <div>
                  <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-2">연결할 포트폴리오 ({portfolios.length}개 자동 연결)</p>
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
                  취소
                </button>
                <button type="submit" className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold hover:scale-[1.02] active:scale-95 transition-transform">
                  생성하기
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
        />
      )}

      {/* GitHub 토큰 설정 모달 */}
      {showTokenModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={() => setShowTokenModal(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}>
            <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white mb-1">GitHub 개인 액세스 토큰</h3>
            <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-4">
              프라이빗 저장소의 AI 분석을 위해 필요합니다.<br />
              GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens 에서 발급하세요.<br />
              <span className="text-xs text-outline dark:text-slate-500">필요 권한: <code>Contents (read)</code></span>
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
                취소
              </button>
              <button onClick={handleSaveToken} disabled={!tokenInput.trim() || tokenSaving}
                className="flex-1 py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-semibold shadow hover:scale-[1.01] active:scale-95 transition-transform disabled:opacity-50">
                {tokenSaving ? '저장중...' : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
