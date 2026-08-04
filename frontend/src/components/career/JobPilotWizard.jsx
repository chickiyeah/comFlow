import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  collectJob, extractJob, matchJob, generateCoverLetter, saveGeneratedCoverLetter,
} from '../../api/jobpilot'
import { checkLength } from '../../utils/counter'

const STATUS_STYLE = {
  ok:       'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  over:     'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
  short:    'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300',
  no_limit: 'bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300',
}
const SEVERITY_STYLE = {
  must:      'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
  preferred: 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-300',
}

const STEPS = ['input', 'review', 'match', 'generate']

export default function JobPilotWizard() {
  const { t } = useTranslation()
  const [step, setStep] = useState(0)
  const [busy, setBusy] = useState('')   // '' | 'fetch' | 'extract' | 'match' | 'generate' | 'save'
  const [error, setError] = useState('')

  // step 1
  const [inputMode, setInputMode] = useState('paste')
  const [rawText, setRawText] = useState('')
  const [url, setUrl] = useState('')

  // step 2~4
  const [job, setJob] = useState(null)
  const [match, setMatch] = useState(null)
  const [report, setReport] = useState(null)
  const [essays, setEssays] = useState([])     // [{question, text, limit, charLimitType}]
  const [savedMsg, setSavedMsg] = useState('')
  const [copiedIdx, setCopiedIdx] = useState(-1)

  const reset = () => {
    setStep(0); setError(''); setRawText(''); setUrl('')
    setJob(null); setMatch(null); setReport(null); setEssays([]); setSavedMsg('')
  }

  // ── [수집] URL fetch ──────────────────────────────────────────
  const handleFetch = async () => {
    setError(''); setBusy('fetch')
    try {
      const res = await collectJob({ url: url.trim() })
      setRawText(res.data?.rawText ?? '')
      setInputMode('paste')   // 가져온 본문을 편집 가능하게
    } catch (e) {
      setError(t('jobpilot.fetchFail'))
    } finally { setBusy('') }
  }

  // ── [추출] 원문 → JobPosting ──────────────────────────────────
  const handleExtract = async () => {
    if (rawText.trim().length < 30) { setError(t('jobpilot.needText')); return }
    setError(''); setBusy('extract')
    try {
      const res = await extractJob(rawText.trim(), url.trim() || null)
      const j = res.data
      // 리스트 필드 null 방어
      j.requiredSkills = j.requiredSkills ?? []
      j.preferred = j.preferred ?? []
      j.essayQuestions = j.essayQuestions ?? []
      setJob(j); setStep(1)
    } catch (e) {
      setError(t('jobpilot.extractFail'))
    } finally { setBusy('') }
  }

  // ── [매칭] ────────────────────────────────────────────────────
  const handleMatch = async () => {
    setError(''); setBusy('match')
    try {
      const res = await matchJob(job, null)   // 프로필은 서버가 조립
      setMatch(res.data); setStep(2)
    } catch (e) {
      setError(t('jobpilot.error'))
    } finally { setBusy('') }
  }

  // ── [생성] ────────────────────────────────────────────────────
  const handleGenerate = async () => {
    setError(''); setBusy('generate'); setSavedMsg('')
    try {
      const res = await generateCoverLetter(job, match, null)
      const rep = res.data
      setReport(rep)
      // essay 에 charLimitType 을 문항에서 매핑(라이브 카운트용)
      setEssays((rep.essays ?? []).map((e, i) => ({
        question: e.question,
        text: e.text,
        limit: e.limit,
        charLimitType: job.essayQuestions?.[i]?.charLimitType ?? null,
      })))
      setStep(3)
    } catch (e) {
      setError(t('jobpilot.error'))
    } finally { setBusy('') }
  }

  // ── 편집 헬퍼 ─────────────────────────────────────────────────
  const setJobField = (k, v) => setJob(p => ({ ...p, [k]: v }))
  const setQuestion = (i, k, v) => setJob(p => {
    const qs = [...p.essayQuestions]; qs[i] = { ...qs[i], [k]: v }; return { ...p, essayQuestions: qs }
  })
  const addQuestion = () => setJob(p => ({
    ...p, essayQuestions: [...(p.essayQuestions ?? []), { question: '', charLimit: null, charLimitType: null }],
  }))
  const removeQuestion = (i) => setJob(p => ({
    ...p, essayQuestions: p.essayQuestions.filter((_, idx) => idx !== i),
  }))
  const setEssayText = (i, v) => setEssays(p => { const n = [...p]; n[i] = { ...n[i], text: v }; return n })

  const copyOne = (text, i) => {
    navigator.clipboard?.writeText(text)
    setCopiedIdx(i); setTimeout(() => setCopiedIdx(-1), 1500)
  }
  const copyAll = () => {
    const all = essays.map(e => (e.question ? `■ ${e.question}\n` : '') + e.text).join('\n\n')
    navigator.clipboard?.writeText(all); setCopiedIdx(-2); setTimeout(() => setCopiedIdx(-1), 1500)
  }
  const download = () => {
    const all = essays.map(e => (e.question ? `■ ${e.question}\n` : '') + e.text).join('\n\n')
    const blob = new Blob([all], { type: 'text/plain;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${job.company || 'jobpilot'}_자기소개서.txt`
    a.click(); URL.revokeObjectURL(a.href)
  }
  const saveToMine = async () => {
    setBusy('save'); setSavedMsg(''); setError('')
    try {
      const content = essays.map(e => (e.question ? `■ ${e.question}\n` : '') + e.text).join('\n\n')
      await saveGeneratedCoverLetter({
        title: `${job.company || t('jobpilot.unknownCompany')} ${t('jobpilot.coverLetter')}`,
        companyName: job.company || t('jobpilot.unknownCompany'),
        jobTitle: job.position || t('jobpilot.unknownPosition'),
        content,
      })
      setSavedMsg(t('jobpilot.saved'))
    } catch (e) {
      setError(t('jobpilot.error'))
    } finally { setBusy('') }
  }

  // ── 렌더 ──────────────────────────────────────────────────────
  return (
    <div className="space-y-6">
      {/* 헤더 + 정직성 안내 */}
      <div className="flex items-center gap-3 p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-2xl border border-secondary-fixed/30">
        <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-3xl">auto_awesome</span>
        <div>
          <p className="font-black text-lg font-['Space_Grotesk'] text-primary dark:text-white">{t('jobpilot.title')}</p>
          <p className="text-label-md text-outline dark:text-slate-400">{t('jobpilot.honestyNote')}</p>
        </div>
      </div>

      {/* 스텝 인디케이터 */}
      <div className="flex items-center gap-2">
        {STEPS.map((s, i) => (
          <div key={s} className="flex items-center gap-2 flex-1">
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold whitespace-nowrap ${
              i === step ? 'bg-primary text-white dark:bg-primary-container'
              : i < step ? 'bg-secondary-container/40 text-primary dark:text-secondary-fixed'
              : 'bg-surface-container dark:bg-slate-800 text-outline dark:text-slate-500'
            }`}>
              <span className="w-5 h-5 rounded-full bg-white/20 flex items-center justify-center text-[11px]">{i + 1}</span>
              {t(`jobpilot.step_${s}`)}
            </div>
            {i < STEPS.length - 1 && <div className="h-px flex-1 bg-outline-variant dark:bg-slate-700" />}
          </div>
        ))}
      </div>

      {error && <p className="text-error text-sm bg-error-container dark:bg-error/20 px-4 py-3 rounded-xl">{error}</p>}

      {/* ── STEP 1: 공고 입력 ── */}
      {step === 0 && (
        <div className="card p-6 space-y-4">
          <div className="flex gap-1 bg-surface-container dark:bg-slate-800 p-1 rounded-xl w-fit">
            {['paste', 'url'].map(m => (
              <button key={m} onClick={() => setInputMode(m)}
                className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all ${
                  inputMode === m ? 'bg-white dark:bg-slate-700 text-primary dark:text-white shadow' : 'text-on-surface-variant dark:text-slate-400'}`}>
                {t(`jobpilot.inputMode_${m}`)}
              </button>
            ))}
          </div>

          {inputMode === 'url' && (
            <div className="flex gap-3">
              <input value={url} onChange={e => setUrl(e.target.value)} placeholder={t('jobpilot.urlPlaceholder')}
                className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <button onClick={handleFetch} disabled={busy === 'fetch' || !url.trim()}
                className="px-5 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shrink-0 disabled:opacity-50">
                {busy === 'fetch' ? t('jobpilot.fetching') : t('jobpilot.fetchBtn')}
              </button>
            </div>
          )}

          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">
              {inputMode === 'url' ? t('jobpilot.orPaste') : t('jobpilot.pasteLabel')}
            </label>
            <textarea value={rawText} onChange={e => setRawText(e.target.value)} rows={9}
              placeholder={t('jobpilot.pastePlaceholder')}
              className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-y" />
          </div>

          <div className="flex justify-end">
            <button onClick={handleExtract} disabled={busy === 'extract' || rawText.trim().length < 30}
              className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 disabled:opacity-50">
              {busy === 'extract' ? t('jobpilot.extracting') : t('jobpilot.extractBtn')}
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 2: 추출 결과 확인·수정 ── */}
      {step === 1 && job && (
        <div className="card p-6 space-y-5">
          <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('jobpilot.reviewHint')}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {[['company', 'fld_company'], ['position', 'fld_position'], ['deadline', 'fld_deadline'], ['employmentType', 'fld_employmentType']].map(([k, lk]) => (
              <div key={k}>
                <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1">{t(`jobpilot.${lk}`)}</label>
                <input value={job[k] ?? ''} onChange={e => setJobField(k, e.target.value)}
                  className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              </div>
            ))}
          </div>

          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1">{t('jobpilot.fld_requiredSkills')}</label>
            <input value={(job.requiredSkills ?? []).join(', ')}
              onChange={e => setJobField('requiredSkills', e.target.value.split(',').map(s => s.trim()).filter(Boolean))}
              placeholder={t('jobpilot.skillsHint')}
              className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
          </div>

          {/* 자소서 문항 */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-label-md font-bold text-primary dark:text-white">{t('jobpilot.essayQuestions')}</label>
              <button onClick={addQuestion} className="text-xs font-bold text-primary dark:text-secondary-fixed flex items-center gap-1">
                <span className="material-symbols-outlined text-[16px]">add</span>{t('jobpilot.addQuestion')}
              </button>
            </div>
            {(job.essayQuestions ?? []).length === 0 && (
              <p className="text-xs text-outline dark:text-slate-500 mb-2">{t('jobpilot.noQuestionsDefault')}</p>
            )}
            <div className="space-y-3">
              {(job.essayQuestions ?? []).map((q, i) => (
                <div key={i} className="p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl border border-outline-variant dark:border-slate-700 space-y-2">
                  <div className="flex gap-2">
                    <input value={q.question ?? ''} onChange={e => setQuestion(i, 'question', e.target.value)}
                      placeholder={t('jobpilot.qPlaceholder')}
                      className="flex-1 px-3 py-2 bg-white dark:bg-slate-900 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                    <button onClick={() => removeQuestion(i)} className="text-error shrink-0 px-1">
                      <span className="material-symbols-outlined text-[20px]">delete</span>
                    </button>
                  </div>
                  <div className="flex gap-2 items-center">
                    <input type="number" value={q.charLimit ?? ''} onChange={e => setQuestion(i, 'charLimit', e.target.value ? Number(e.target.value) : null)}
                      placeholder={t('jobpilot.charLimit')}
                      className="w-28 px-3 py-1.5 bg-white dark:bg-slate-900 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
                    <select value={q.charLimitType ?? ''} onChange={e => setQuestion(i, 'charLimitType', e.target.value || null)}
                      className="px-3 py-1.5 bg-white dark:bg-slate-900 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/30">
                      <option value="">{t('jobpilot.noLimitType')}</option>
                      <option value="공백포함">{t('jobpilot.withSpaces')}</option>
                      <option value="공백제외">{t('jobpilot.withoutSpaces')}</option>
                    </select>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-between pt-2">
            <button onClick={() => setStep(0)} className="px-5 py-2.5 text-sm font-semibold text-on-surface-variant dark:text-slate-400">{t('jobpilot.back')}</button>
            <button onClick={handleMatch} disabled={busy === 'match'}
              className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm disabled:opacity-50">
              {busy === 'match' ? t('jobpilot.matching') : t('jobpilot.runMatch')}
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 3: 매칭 결과 ── */}
      {step === 2 && match && (
        <div className="card p-6 space-y-5">
          <p className="text-sm font-semibold text-primary dark:text-white">{match.summary}</p>

          <div>
            <p className="text-label-md font-bold text-green-700 dark:text-green-400 mb-2 flex items-center gap-1">
              <span className="material-symbols-outlined text-[18px]">check_circle</span>{t('jobpilot.strengths')}
            </p>
            <div className="space-y-1.5">
              {(match.strengths ?? []).length === 0 && <p className="text-xs text-outline dark:text-slate-500">—</p>}
              {(match.strengths ?? []).map((s, i) => (
                <div key={i} className="flex items-start gap-2 text-sm">
                  <span className="px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs font-bold shrink-0">{s.skill}</span>
                  <span className="text-on-surface-variant dark:text-slate-400 text-xs pt-0.5">{s.evidence}</span>
                </div>
              ))}
            </div>
          </div>

          <div>
            <p className="text-label-md font-bold text-red-700 dark:text-red-400 mb-2 flex items-center gap-1">
              <span className="material-symbols-outlined text-[18px]">priority_high</span>{t('jobpilot.gaps')}
            </p>
            <div className="flex flex-wrap gap-2">
              {(match.gaps ?? []).length === 0 && <p className="text-xs text-outline dark:text-slate-500">{t('jobpilot.noGaps')}</p>}
              {(match.gaps ?? []).map((g, i) => (
                <span key={i} className={`px-2.5 py-1 rounded-full text-xs font-bold ${SEVERITY_STYLE[g.severity] ?? SEVERITY_STYLE.preferred}`}>
                  {g.skill} · {t(`jobpilot.sev_${g.severity}`)}
                </span>
              ))}
            </div>
          </div>

          <div className="flex justify-between pt-2">
            <button onClick={() => setStep(1)} className="px-5 py-2.5 text-sm font-semibold text-on-surface-variant dark:text-slate-400">{t('jobpilot.back')}</button>
            <button onClick={handleGenerate} disabled={busy === 'generate'}
              className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 disabled:opacity-50">
              {busy === 'generate' ? t('jobpilot.generating') : t('jobpilot.genBtn')}
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 4: 생성 결과 + 검토 ── */}
      {step === 3 && report && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2 justify-between">
            <p className="text-sm text-on-surface-variant dark:text-slate-400">
              {t('jobpilot.flavorLabel')}: <span className="font-bold text-primary dark:text-white">{report.flavorLabel}</span>
            </p>
            <div className="flex gap-2">
              <button onClick={copyAll} className="px-3 py-2 text-xs font-bold rounded-lg bg-surface-container dark:bg-slate-800 text-primary dark:text-white flex items-center gap-1">
                <span className="material-symbols-outlined text-[16px]">content_copy</span>{copiedIdx === -2 ? t('jobpilot.copied') : t('jobpilot.copyAll')}
              </button>
              <button onClick={download} className="px-3 py-2 text-xs font-bold rounded-lg bg-surface-container dark:bg-slate-800 text-primary dark:text-white flex items-center gap-1">
                <span className="material-symbols-outlined text-[16px]">download</span>{t('jobpilot.download')}
              </button>
              <button onClick={saveToMine} disabled={busy === 'save'}
                className="px-3 py-2 text-xs font-bold rounded-lg bg-primary dark:bg-primary-container text-white flex items-center gap-1 disabled:opacity-50">
                <span className="material-symbols-outlined text-[16px]">bookmark_add</span>
                {busy === 'save' ? t('jobpilot.saving') : t('jobpilot.saveToMine')}
              </button>
            </div>
          </div>
          {savedMsg && <p className="text-green-700 dark:text-green-400 text-sm bg-green-100 dark:bg-green-900/20 px-4 py-2 rounded-xl">{savedMsg}</p>}

          {essays.map((e, i) => {
            const v = checkLength(e.text, e.limit, e.charLimitType)
            return (
              <div key={i} className="card p-5 space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <p className="font-bold text-primary dark:text-white text-sm">{e.question}</p>
                  <button onClick={() => copyOne(e.text, i)} className="text-outline dark:text-slate-400 shrink-0">
                    <span className="material-symbols-outlined text-[18px]">{copiedIdx === i ? 'check' : 'content_copy'}</span>
                  </button>
                </div>
                <textarea value={e.text} onChange={ev => setEssayText(i, ev.target.value)} rows={7}
                  className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/30 resize-y" />
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  <span className={`px-2 py-0.5 rounded-full font-bold ${STATUS_STYLE[v.status]}`}>{t(`jobpilot.st_${v.status}`)}</span>
                  {e.limit && <span className="text-outline dark:text-slate-400">{t('jobpilot.limitLabel')}: {e.limit}{t('jobpilot.charsUnit')}</span>}
                  <span className="text-on-surface-variant dark:text-slate-400">{t('jobpilot.withSpaces')} {checkLength(e.text, null).count}{t('jobpilot.charsUnit')}</span>
                  <span className="text-on-surface-variant dark:text-slate-400">/ {t('jobpilot.withoutSpaces')} {e.text.replace(/\s/g, '').length}{t('jobpilot.charsUnit')}</span>
                </div>
              </div>
            )
          })}

          <div className="flex justify-between pt-2">
            <button onClick={() => setStep(2)} className="px-5 py-2.5 text-sm font-semibold text-on-surface-variant dark:text-slate-400">{t('jobpilot.back')}</button>
            <div className="flex gap-2">
              <button onClick={handleGenerate} disabled={busy === 'generate'}
                className="px-5 py-2.5 text-sm font-bold rounded-xl bg-surface-container dark:bg-slate-800 text-primary dark:text-white disabled:opacity-50">
                {busy === 'generate' ? t('jobpilot.generating') : t('jobpilot.regenerateAll')}
              </button>
              <button onClick={reset} className="px-5 py-2.5 text-sm font-bold rounded-xl bg-primary dark:bg-primary-container text-white">{t('jobpilot.restart')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
