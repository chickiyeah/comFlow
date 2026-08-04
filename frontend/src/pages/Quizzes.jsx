import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import useAuthStore from '../store/authStore'
import { getQuizzes, getQuiz, createQuiz, deleteQuiz, submitQuiz, getQuizSubmissions, generateQuiz } from '../api/quiz'

const EMPTY_Q = { type: 'MCQ', text: '', options: ['', ''], correctAnswer: '0', points: 5 }
const EMPTY_QUIZ = { courseId: null, title: '', description: '', active: true, questions: [{ ...EMPTY_Q }] }

export default function Quizzes() {
  const { t } = useTranslation()
  const user = useAuthStore(s => s.user)
  const isStaff = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_PROFESSOR'

  const [quizzes, setQuizzes] = useState([])
  const [loading, setLoading] = useState(true)

  // 응시
  const [taking, setTaking] = useState(null)     // {id,title,description,questions}
  const [answers, setAnswers] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)

  // 생성
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_QUIZ)
  const [saving, setSaving] = useState(false)
  const [genTopic, setGenTopic] = useState('')
  const [genCount, setGenCount] = useState(5)
  const [genType, setGenType] = useState('MIX')
  const [generating, setGenerating] = useState(false)

  // 제출현황
  const [subsFor, setSubsFor] = useState(null)
  const [subs, setSubs] = useState([])

  const load = () => {
    setLoading(true)
    getQuizzes().then(r => setQuizzes(r.data ?? [])).catch(() => setQuizzes([])).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const openTake = async (q) => {
    setResult(null); setAnswers({})
    try { const r = await getQuiz(q.id); setTaking(r.data) } catch { /* ignore */ }
  }
  const handleSubmit = async () => {
    if (!taking || submitting) return
    setSubmitting(true)
    try {
      const payload = taking.questions.map(q => ({ questionId: q.id, response: answers[q.id] ?? '' }))
      const r = await submitQuiz(taking.id, payload)
      setResult(r.data)
    } catch { alert(t('quiz.submitFailed')) }
    finally { setSubmitting(false) }
  }

  const handleDelete = async (id) => {
    if (!confirm(t('quiz.confirmDelete'))) return
    try { await deleteQuiz(id); setQuizzes(prev => prev.filter(q => q.id !== id)) } catch { /* ignore */ }
  }
  const openSubs = async (q) => {
    setSubsFor(q); setSubs([])
    try { const r = await getQuizSubmissions(q.id); setSubs(r.data ?? []) } catch { setSubs([]) }
  }

  // 생성 폼 조작
  const setQ = (i, patch) => setForm(f => ({ ...f, questions: f.questions.map((q, idx) => idx === i ? { ...q, ...patch } : q) }))
  const addQ = () => setForm(f => ({ ...f, questions: [...f.questions, { ...EMPTY_Q }] }))
  const removeQ = (i) => setForm(f => ({ ...f, questions: f.questions.length > 1 ? f.questions.filter((_, idx) => idx !== i) : f.questions }))
  const setOpt = (qi, oi, val) => setQ(qi, { options: form.questions[qi].options.map((o, idx) => idx === oi ? val : o) })
  const addOpt = (qi) => setQ(qi, { options: [...form.questions[qi].options, ''] })
  const handleCreate = async (e) => {
    e.preventDefault()
    if (!form.title.trim() || saving) return
    setSaving(true)
    try { await createQuiz(form); setShowForm(false); setForm(EMPTY_QUIZ); load() }
    catch { alert(t('quiz.saveFailed')) }
    finally { setSaving(false) }
  }

  const handleGenerate = async () => {
    if (!genTopic.trim() || generating) return
    setGenerating(true)
    try {
      const res = await generateQuiz({ topic: genTopic, count: Number(genCount) || 5, type: genType })
      const d = res.data?.data ?? res.data
      const qs = (d.questions || []).map(q => ({
        type: q.type || 'MCQ',
        text: q.text || '',
        options: q.options && q.options.length ? q.options : ['', ''],
        correctAnswer: q.correctAnswer ?? '0',
        points: q.points ?? 5,
      }))
      if (!qs.length) { alert(t('quiz.genFailed')); return }
      setForm(f => ({ ...f, title: f.title || d.title || genTopic, questions: qs }))
    } catch { alert(t('quiz.genFailed')) }
    finally { setGenerating(false) }
  }

  return (
    <Layout title={t('quiz.title')}>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary-fixed">quiz</span>{t('quiz.title')}
          </h2>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('quiz.subtitle')}</p>
        </div>
        {isStaff && (
          <button onClick={() => { setForm({ ...EMPTY_QUIZ, questions: [{ ...EMPTY_Q }] }); setShowForm(true) }}
            className="px-4 py-2.5 rounded-xl text-sm font-bold bg-secondary-fixed text-primary flex items-center gap-1.5 shrink-0">
            <span className="material-symbols-outlined text-[18px]">add</span>{t('quiz.create')}
          </button>
        )}
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
      ) : quizzes.length === 0 ? (
        <div className="card p-12 text-center">
          <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600 mb-3">quiz</span>
          <p className="font-bold text-primary dark:text-white">{t('quiz.emptyTitle')}</p>
          <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1">{t('quiz.emptySubtitle')}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {quizzes.map(q => (
            <div key={q.id} className="card p-5 flex flex-col">
              <p className="font-bold text-primary dark:text-white">{q.title}</p>
              {q.description && <p className="text-sm text-on-surface-variant dark:text-slate-400 mt-1 line-clamp-2">{q.description}</p>}
              <p className="text-xs text-outline dark:text-slate-500 mt-2">{q.instructorName} · {t('quiz.meta', { count: q.questionCount, points: q.totalPoints })}</p>
              <div className="mt-auto pt-3 flex items-center gap-2">
                <button onClick={() => openTake(q)} className="flex-1 py-2 rounded-lg bg-primary dark:bg-primary-container text-white text-sm font-bold">{t('quiz.take')}</button>
                {isStaff && (
                  <>
                    <button onClick={() => openSubs(q)} className="p-2 rounded-lg bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300" title={t('quiz.submissions')}>
                      <span className="material-symbols-outlined text-[18px]">grading</span>
                    </button>
                    <button onClick={() => handleDelete(q.id)} className="p-2 rounded-lg bg-error-container dark:bg-error/20 text-error">
                      <span className="material-symbols-outlined text-[18px]">delete</span>
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 응시 모달 */}
      {taking && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setTaking(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[92vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{taking.title}</h3>
              <button onClick={() => setTaking(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <div className="p-6 space-y-5">
              {result ? (
                <div className="space-y-4">
                  <div className="text-center py-4">
                    <p className="text-sm text-outline dark:text-slate-400">{t('quiz.yourScore')}</p>
                    <p className="text-4xl font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk']">{result.score}<span className="text-xl text-outline"> / {result.maxScore}</span></p>
                    {result.needsReview && <p className="text-xs text-yellow-600 dark:text-yellow-400 mt-1">{t('quiz.needsReview')}</p>}
                  </div>
                  {result.answers.map((a, i) => (
                    <div key={i} className="bg-surface-container-low dark:bg-slate-800 rounded-xl px-4 py-2.5">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-bold text-on-surface dark:text-white">Q{i + 1}</span>
                        <span className={`text-sm font-bold ${a.awardedScore === a.points ? 'text-green-600 dark:text-green-400' : a.awardedScore > 0 ? 'text-yellow-600 dark:text-yellow-400' : 'text-error'}`}>{a.awardedScore} / {a.points}</span>
                      </div>
                      {a.feedback && <p className="text-xs text-on-surface-variant dark:text-slate-400 mt-1">{a.feedback}</p>}
                    </div>
                  ))}
                  <button onClick={() => setTaking(null)} className="w-full py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold">{t('quiz.done')}</button>
                </div>
              ) : (
                <>
                  {taking.description && <p className="text-sm text-on-surface-variant dark:text-slate-400">{taking.description}</p>}
                  {taking.questions.map((q, i) => (
                    <div key={q.id}>
                      <p className="font-bold text-sm text-primary dark:text-white mb-2">Q{i + 1}. {q.text} <span className="text-xs text-outline dark:text-slate-500">({q.points}{t('quiz.pt')})</span></p>
                      {q.type === 'MCQ' ? (
                        <div className="space-y-1.5">
                          {q.options.map((opt, oi) => (
                            <label key={oi} className={`flex items-center gap-2.5 px-3 py-2 rounded-lg border cursor-pointer text-sm ${answers[q.id] === String(oi) ? 'border-primary bg-primary/5 dark:bg-primary-container/20' : 'border-outline-variant dark:border-slate-700'}`}>
                              <input type="radio" name={`q${q.id}`} checked={answers[q.id] === String(oi)} onChange={() => setAnswers(a => ({ ...a, [q.id]: String(oi) }))} className="accent-primary" />
                              <span className="text-on-surface dark:text-slate-200">{opt}</span>
                            </label>
                          ))}
                        </div>
                      ) : (
                        <textarea value={answers[q.id] ?? ''} onChange={e => setAnswers(a => ({ ...a, [q.id]: e.target.value }))} rows={3}
                          placeholder={t('quiz.shortPlaceholder')}
                          className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none" />
                      )}
                    </div>
                  ))}
                  <button onClick={handleSubmit} disabled={submitting} className="w-full py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50">
                    {submitting ? t('quiz.grading') : t('quiz.submit')}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 제출현황 모달 (교직원) */}
      {subsFor && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setSubsFor(null)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl max-h-[80vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('quiz.submissions')} — {subsFor.title}</h3>
              <button onClick={() => setSubsFor(null)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <div className="p-5">
              {subs.length === 0 ? (
                <p className="text-sm text-outline dark:text-slate-500 text-center py-6">{t('quiz.noSubmissions')}</p>
              ) : (
                <ul className="space-y-2">
                  {subs.map(s => (
                    <li key={s.id} className="flex items-center justify-between px-4 py-3 rounded-xl bg-surface-container-low dark:bg-slate-800">
                      <span className="text-sm font-medium text-on-surface dark:text-white">{s.studentName}</span>
                      <span className="flex items-center gap-2">
                        {s.needsReview && <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400">{t('quiz.review')}</span>}
                        <span className="text-sm font-bold text-primary dark:text-secondary-fixed">{s.score} / {s.maxScore}</span>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 생성 모달 (교직원) */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 z-[60] flex items-end sm:items-center justify-center p-4" onClick={e => e.target === e.currentTarget && setShowForm(false)}>
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg shadow-2xl max-h-[92vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 bg-white dark:bg-slate-900 z-10">
              <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white">{t('quiz.create')}</h3>
              <button onClick={() => setShowForm(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800">
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <input required value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder={t('quiz.fTitle')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <input value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} placeholder={t('quiz.fDesc')}
                className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />

              {/* AI 자동 출제 */}
              <div className="rounded-xl border border-secondary-fixed/40 bg-secondary-container/15 dark:bg-secondary-fixed/5 p-3 space-y-2">
                <p className="text-xs font-bold text-primary dark:text-secondary-fixed flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px]">auto_awesome</span>{t('quiz.aiGen')}
                </p>
                <div className="flex gap-2">
                  <input value={genTopic} onChange={e => setGenTopic(e.target.value)} placeholder={t('quiz.aiTopic')}
                    className="flex-1 px-3 py-2 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm" />
                  <input type="number" min="1" max="10" value={genCount} onChange={e => setGenCount(e.target.value)} title={t('quiz.aiCount')}
                    className="w-14 px-2 py-2 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm" />
                  <select value={genType} onChange={e => setGenType(e.target.value)}
                    className="px-2 py-2 bg-white dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-white rounded-lg text-xs font-bold">
                    <option value="MIX">{t('quiz.aiMix')}</option>
                    <option value="MCQ">{t('quiz.typeMcq')}</option>
                    <option value="SHORT">{t('quiz.typeShort')}</option>
                  </select>
                </div>
                <button type="button" onClick={handleGenerate} disabled={generating || !genTopic.trim()}
                  className="w-full py-2 rounded-lg bg-primary dark:bg-primary-container text-white text-sm font-bold disabled:opacity-50 flex items-center justify-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px]">auto_awesome</span>{generating ? t('quiz.aiGenerating') : t('quiz.aiGenerate')}
                </button>
              </div>

              {form.questions.map((q, qi) => (
                <div key={qi} className="border border-outline-variant dark:border-slate-700 rounded-xl p-3 space-y-2">
                  <div className="flex items-center gap-2">
                    <select value={q.type} onChange={e => setQ(qi, { type: e.target.value })}
                      className="px-2 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-xs font-bold text-primary dark:text-white border border-outline-variant dark:border-slate-700">
                      <option value="MCQ">{t('quiz.typeMcq')}</option>
                      <option value="SHORT">{t('quiz.typeShort')}</option>
                    </select>
                    <input type="number" min="1" value={q.points} onChange={e => setQ(qi, { points: Number(e.target.value) })}
                      className="w-16 px-2 py-1.5 rounded-lg bg-surface-container dark:bg-slate-800 text-xs text-on-surface dark:text-white border border-outline-variant dark:border-slate-700" title={t('quiz.points')} />
                    <span className="text-xs text-outline dark:text-slate-500">{t('quiz.pt')}</span>
                    <button type="button" onClick={() => removeQ(qi)} className="ml-auto text-outline dark:text-slate-500 hover:text-error"><span className="material-symbols-outlined text-[18px]">delete</span></button>
                  </div>
                  <input required value={q.text} onChange={e => setQ(qi, { text: e.target.value })} placeholder={t('quiz.qText')}
                    className="w-full px-3 py-2 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm" />
                  {q.type === 'MCQ' ? (
                    <div className="space-y-1.5">
                      {q.options.map((opt, oi) => (
                        <div key={oi} className="flex items-center gap-2">
                          <input type="radio" name={`correct${qi}`} checked={q.correctAnswer === String(oi)} onChange={() => setQ(qi, { correctAnswer: String(oi) })} className="accent-primary" title={t('quiz.markCorrect')} />
                          <input value={opt} onChange={e => setOpt(qi, oi, e.target.value)} placeholder={`${t('quiz.option')} ${oi + 1}`}
                            className="flex-1 px-3 py-1.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-lg text-sm" />
                        </div>
                      ))}
                      <button type="button" onClick={() => addOpt(qi)} className="text-xs font-semibold text-primary dark:text-secondary-fixed flex items-center gap-1"><span className="material-symbols-outlined text-[14px]">add</span>{t('quiz.addOption')}</button>
                    </div>
                  ) : (
                    <input value={q.correctAnswer ?? ''} onChange={e => setQ(qi, { correctAnswer: e.target.value })} placeholder={t('quiz.modelAnswer')}
                      className="w-full px-3 py-2 bg-secondary-container/20 dark:bg-secondary-fixed/10 border border-secondary-fixed/30 dark:text-on-surface rounded-lg text-sm" />
                  )}
                </div>
              ))}
              <button type="button" onClick={addQ} className="w-full py-2 rounded-xl border border-dashed border-outline-variant dark:border-slate-700 text-sm font-semibold text-primary dark:text-secondary-fixed flex items-center justify-center gap-1">
                <span className="material-symbols-outlined text-[18px]">add</span>{t('quiz.addQuestion')}
              </button>
              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => setShowForm(false)} className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-sm font-bold">{t('common.cancel')}</button>
                <button type="submit" disabled={saving} className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-sm font-bold disabled:opacity-50">{saving ? t('quiz.saving') : t('quiz.submitCreate')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  )
}
