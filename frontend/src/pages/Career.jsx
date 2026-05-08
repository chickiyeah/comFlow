import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { generateRoadmap } from '../api/roadmap'

const CERT_TYPE_STYLE = {
  REQUIRED:    'bg-error-container dark:bg-error/20 text-error border-error/30',
  RECOMMENDED: 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed border-secondary-fixed/30',
  OPTIONAL:    'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 border-outline-variant',
}
const CERT_TYPE_LABEL = { REQUIRED: '필수', RECOMMENDED: '권장', OPTIONAL: '선택' }

export default function Career() {
  const { t } = useTranslation()
  const [jobTitle, setJobTitle] = useState('')
  const [useExternal, setUseExternal] = useState(true)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleGenerate = async (e) => {
    e.preventDefault()
    if (!jobTitle.trim()) return
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await generateRoadmap(jobTitle.trim(), useExternal)
      setResult(res.data)
    } catch {
      setError('로드맵 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Layout title={t('career.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('career.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('career.subtitle')}</p>
      </div>

      {/* Input card */}
      <div className="card p-6 mb-6">
        <form onSubmit={handleGenerate} className="space-y-4">
          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">{t('career.title')}</label>
            <div className="flex gap-3">
              <input
                value={jobTitle}
                onChange={e => setJobTitle(e.target.value)}
                placeholder={t('career.jobPlaceholder')}
                className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
              />
              <button
                type="submit"
                disabled={loading || !jobTitle.trim()}
                className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
              >
                {loading ? t('career.generating') : t('career.generateBtn')}
              </button>
            </div>
          </div>

          {/* AI mode toggle */}
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

        {error && (
          <p className="mt-4 text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{error}</p>
        )}
      </div>

      {/* Loading skeleton */}
      {loading && (
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

      {/* Result */}
      {result && !loading && (
        <div className="space-y-6">
          <div className="flex items-center gap-3 p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30">
            <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-3xl">route</span>
            <div>
              <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">{result.jobTitle} 로드맵</p>
              <p className="text-label-md text-outline dark:text-slate-400">AI 출처: {result.aiSource === 'EXTERNAL' ? '외부 AI (최신 트렌드 반영)' : '내부 AI (학과 자료 기반)'}</p>
            </div>
          </div>

          {/* Certificates */}
          <div className="card p-6">
            <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>추천 자격증
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {result.certificates?.map((c, i) => (
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

          {/* Semester plans */}
          <div className="card p-6">
            <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">timeline</span>4학기 학습 로드맵
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {result.semesterPlans?.map((p, i) => (
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

      {/* Empty state */}
      {!result && !loading && !error && (
        <div className="card p-12 text-center">
          <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600 mb-4">work_outline</span>
          <p className="text-xl font-bold text-primary dark:text-white font-['Space_Grotesk']">희망 직업을 입력해보세요</p>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2">AI가 맞춤형 2년 4학기 학습 경로와 자격증을 추천합니다.</p>
        </div>
      )}
    </Layout>
  )
}
