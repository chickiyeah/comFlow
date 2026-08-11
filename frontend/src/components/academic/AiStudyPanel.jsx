import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { generateStudyPlan, predictGrade } from '../../api/planner'

export default function AiStudyPanel() {
  const { t } = useTranslation()

  // ── 공부 플래너 ──
  const [exams, setExams] = useState([{ subject: '', examDate: '' }])
  const [weak, setWeak] = useState('')
  const [planLoading, setPlanLoading] = useState(false)
  const [plan, setPlan] = useState(null)
  const [planErr, setPlanErr] = useState('')

  const setExam = (i, k, v) => setExams(arr => arr.map((e, idx) => idx === i ? { ...e, [k]: v } : e))
  const addExam = () => setExams(arr => [...arr, { subject: '', examDate: '' }])
  const removeExam = (i) => setExams(arr => arr.length > 1 ? arr.filter((_, idx) => idx !== i) : arr)

  const runPlan = async (e) => {
    e.preventDefault()
    setPlanLoading(true); setPlanErr(''); setPlan(null)
    try {
      const payload = {
        exams: exams.filter(x => x.subject.trim() && x.examDate),
        weakSubjects: weak.split(',').map(s => s.trim()).filter(Boolean),
      }
      const res = await generateStudyPlan(payload)
      setPlan(res.data)
    } catch { setPlanErr(t('academic.planner.error')) }
    finally { setPlanLoading(false) }
  }

  // ── 성적 예측기 ──
  const [pf, setPf] = useState({ subjectName: '', attendanceRate: 90, midtermScore: 70, midtermWeight: 30, finalWeight: 40 })
  const [predLoading, setPredLoading] = useState(false)
  const [pred, setPred] = useState(null)
  const [predErr, setPredErr] = useState('')

  const runPredict = async (e) => {
    e.preventDefault()
    setPredLoading(true); setPredErr(''); setPred(null)
    try {
      const res = await predictGrade({
        subjectName: pf.subjectName.trim(),
        attendanceRate: Number(pf.attendanceRate),
        midtermScore: Number(pf.midtermScore),
        midtermWeight: Number(pf.midtermWeight),
        finalWeight: Number(pf.finalWeight),
      })
      setPred(res.data)
    } catch { setPredErr(t('academic.predict.error')) }
    finally { setPredLoading(false) }
  }

  const inputCls = 'w-full px-3 py-2.5 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30'

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
      {/* 공부 플래너 */}
      <div className="card p-6">
        <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-1 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">edit_calendar</span>{t('academic.planner.title')}
        </h3>
        <p className="text-label-md text-outline dark:text-slate-400 mb-4">{t('academic.planner.subtitle')}</p>
        <form onSubmit={runPlan} className="space-y-3">
          <div className="space-y-2">
            <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('academic.planner.exams')}</label>
            {exams.map((ex, i) => (
              <div key={i} className="flex gap-2">
                <input value={ex.subject} onChange={e => setExam(i, 'subject', e.target.value)} placeholder={t('academic.planner.examSubject')} className={inputCls} />
                <input type="date" value={ex.examDate} onChange={e => setExam(i, 'examDate', e.target.value)} className={`${inputCls} max-w-[150px]`} />
                <button type="button" onClick={() => removeExam(i)} className="px-2 text-outline dark:text-slate-500 hover:text-error shrink-0">
                  <span className="material-symbols-outlined text-[20px]">remove_circle_outline</span>
                </button>
              </div>
            ))}
            <button type="button" onClick={addExam} className="text-xs font-semibold text-primary dark:text-secondary-fixed flex items-center gap-1">
              <span className="material-symbols-outlined text-[16px]">add</span>{t('academic.planner.addExam')}
            </button>
          </div>
          <div>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1">{t('academic.planner.weak')}</label>
            <input value={weak} onChange={e => setWeak(e.target.value)} placeholder={t('academic.planner.weakPlaceholder')} className={inputCls} />
          </div>
          <button type="submit" disabled={planLoading}
            className="w-full py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm disabled:opacity-50">
            {planLoading ? t('academic.planner.generating') : t('academic.planner.generate')}
          </button>
        </form>
        {planErr && <p className="mt-3 text-error text-sm">{planErr}</p>}
        {plan && (
          <div className="mt-4 space-y-3">
            {(plan.weeklyPlan ?? []).length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead><tr className="text-label-md text-outline dark:text-slate-500 border-b border-slate-100 dark:border-slate-800">
                    <th className="py-2 pr-2">{t('academic.planner.date')}</th><th className="py-2 px-2">{t('academic.planner.subject')}</th><th className="py-2 px-2">{t('academic.planner.task')}</th><th className="py-2 pl-2 text-center">{t('academic.planner.hours')}</th>
                  </tr></thead>
                  <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                    {plan.weeklyPlan.map((w, i) => (
                      <tr key={i}>
                        <td className="py-2 pr-2 text-outline dark:text-slate-400 whitespace-nowrap">{w.date}</td>
                        <td className="py-2 px-2 font-medium text-on-surface dark:text-white">{w.subject}</td>
                        <td className="py-2 px-2 text-on-surface-variant dark:text-slate-300">{w.task}</td>
                        <td className="py-2 pl-2 text-center text-primary dark:text-secondary-fixed font-bold">{w.hours}h</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {(plan.tips ?? []).length > 0 && (
              <div className="bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-xl p-3">
                <p className="text-xs font-bold text-primary dark:text-secondary-fixed mb-1.5">{t('academic.planner.tips')}</p>
                <ul className="space-y-1">
                  {plan.tips.map((tip, i) => <li key={i} className="text-sm text-on-surface dark:text-slate-300 flex gap-2"><span className="text-secondary dark:text-secondary-fixed">•</span>{tip}</li>)}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 성적 예측기 */}
      <div className="card p-6">
        <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-1 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">query_stats</span>{t('academic.predict.title')}
        </h3>
        <p className="text-label-md text-outline dark:text-slate-400 mb-4">{t('academic.predict.subtitle')}</p>
        <form onSubmit={runPredict} className="space-y-3">
          <input required value={pf.subjectName} onChange={e => setPf(f => ({ ...f, subjectName: e.target.value }))} placeholder={t('academic.predict.subjectPlaceholder')} className={inputCls} />
          <div className="grid grid-cols-2 gap-3">
            <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('academic.predict.attendance')}
              <input type="number" min="0" max="100" value={pf.attendanceRate} onChange={e => setPf(f => ({ ...f, attendanceRate: e.target.value }))} className={`${inputCls} mt-1`} />
            </label>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('academic.predict.midScore')}
              <input type="number" min="0" max="100" value={pf.midtermScore} onChange={e => setPf(f => ({ ...f, midtermScore: e.target.value }))} className={`${inputCls} mt-1`} />
            </label>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('academic.predict.midWeight')}
              <input type="number" min="0" max="100" value={pf.midtermWeight} onChange={e => setPf(f => ({ ...f, midtermWeight: e.target.value }))} className={`${inputCls} mt-1`} />
            </label>
            <label className="text-label-md text-on-surface-variant dark:text-slate-400">{t('academic.predict.finalWeight')}
              <input type="number" min="0" max="100" value={pf.finalWeight} onChange={e => setPf(f => ({ ...f, finalWeight: e.target.value }))} className={`${inputCls} mt-1`} />
            </label>
          </div>
          <button type="submit" disabled={predLoading}
            className="w-full py-2.5 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm disabled:opacity-50">
            {predLoading ? t('academic.predict.calculating') : t('academic.predict.calculate')}
          </button>
        </form>
        {predErr && <p className="mt-3 text-error text-sm">{predErr}</p>}
        {pred && (
          <div className="mt-4 space-y-3">
            <div className="flex items-center justify-between bg-surface-container-low dark:bg-slate-800 rounded-xl px-4 py-3">
              <span className="text-sm text-on-surface-variant dark:text-slate-400">{t('academic.predict.estimated')}</span>
              <span className="text-2xl font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk']">{pred.estimatedTotal}</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead><tr className="text-label-md text-outline dark:text-slate-500 border-b border-slate-100 dark:border-slate-800">
                  <th className="py-2 pr-2">{t('academic.predict.targetGrade')}</th><th className="py-2 px-2 text-center">{t('academic.predict.targetTotal')}</th><th className="py-2 pl-2 text-center">{t('academic.predict.finalNeeded')}</th>
                </tr></thead>
                <tbody className="divide-y divide-slate-50 dark:divide-slate-800">
                  {(pred.scenarios ?? []).map((s, i) => (
                    <tr key={i}>
                      <td className="py-2 pr-2 font-bold text-on-surface dark:text-white">{s.targetGrade}</td>
                      <td className="py-2 px-2 text-center text-outline dark:text-slate-400">{s.targetTotal}</td>
                      <td className="py-2 pl-2 text-center font-bold text-primary dark:text-secondary-fixed">{s.finalNeeded}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {pred.attendanceWarning && (
              <p className="text-sm text-error bg-error-container dark:bg-error/20 rounded-xl px-3 py-2">{pred.attendanceWarning}</p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
