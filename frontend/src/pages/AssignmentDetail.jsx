import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import {
  getAssignment, submitAssignment, getSubmissions,
  gradeSubmission, returnSubmission, aiCheckSubmission,
} from '../api/assignment'

const STATUS_STYLE = {
  TURNED_IN: 'bg-primary-container text-on-primary-container',
  LATE: 'bg-warning-bg text-warning-text',
  GRADED: 'bg-success-bg text-success-text',
  RETURNED: 'bg-danger-bg text-danger-text',
}

export default function AssignmentDetail() {
  const { assignmentId } = useParams()
  const { t } = useTranslation()
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submissions, setSubmissions] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAssignment(assignmentId)
      setDetail(res.data)
      if (res.data?.amTeacher) {
        const subs = await getSubmissions(assignmentId)
        setSubmissions(subs.data || [])
      }
    } finally { setLoading(false) }
  }, [assignmentId])

  useEffect(() => { load() }, [load])

  if (loading) return <Layout><p className="text-center text-text-muted py-12">{t('common.loading', '불러오는 중…')}</p></Layout>
  if (!detail) return <Layout><p className="text-center text-text-muted py-12">-</p></Layout>

  return (
    <Layout>
      <div className="max-w-3xl mx-auto space-y-5">
        <div className="card p-6">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="font-space text-xl font-bold text-primary dark:text-white">{detail.title}</h1>
              {detail.dueDate && (
                <p className="text-sm text-text-muted mt-1">
                  {t('assignment.due', '마감')}: {new Date(detail.dueDate).toLocaleString()}
                </p>
              )}
            </div>
            <span className="chip">{detail.points}{t('assignment.points', '점')}</span>
          </div>
          {detail.instructions && (
            <p className="text-body-md text-on-surface dark:text-[#e6e6f5] whitespace-pre-wrap mt-4">
              {detail.instructions}
            </p>
          )}
        </div>

        {detail.stats && (
          <div className="card p-4 grid grid-cols-4 gap-2 text-center">
            <Stat label={t('assignment.total', '전체')} value={detail.stats.totalStudents} />
            <Stat label={t('assignment.submitted', '제출')} value={detail.stats.submitted} />
            <Stat label={t('assignment.graded', '채점')} value={detail.stats.graded} />
            <Stat label={t('assignment.returned', '반려')} value={detail.stats.returned} />
          </div>
        )}

        {!detail.amTeacher && (
          <StudentSubmitPanel assignmentId={assignmentId} submission={detail.mySubmission} onChange={load} />
        )}

        {detail.amTeacher && submissions && (
          <div className="space-y-3">
            <h2 className="font-semibold text-on-surface dark:text-white">{t('assignment.submissions', '제출물')}</h2>
            {submissions.length === 0 ? (
              <p className="text-center text-text-muted py-8">{t('assignment.noSubmissions', '제출물이 없습니다.')}</p>
            ) : submissions.map(s => (
              <TeacherSubmissionRow key={s.id} submission={s} onChange={load} />
            ))}
          </div>
        )}
      </div>
    </Layout>
  )
}

function Stat({ label, value }) {
  return (
    <div>
      <p className="text-title-lg font-bold text-primary dark:text-white">{value}</p>
      <p className="text-xs text-text-muted">{label}</p>
    </div>
  )
}

function StudentSubmitPanel({ assignmentId, submission, onChange }) {
  const { t } = useTranslation()
  const [content, setContent] = useState(submission?.content || '')
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    try {
      await submitAssignment(assignmentId, { content, file })
      setFile(null)
      onChange()
    } finally { setBusy(false) }
  }

  return (
    <div className="card p-5">
      <h2 className="font-semibold text-on-surface dark:text-white mb-3">{t('assignment.mySubmission', '내 제출')}</h2>
      {submission && (
        <div className="mb-3">
          <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${STATUS_STYLE[submission.status] || ''}`}>
            {submission.status}
          </span>
          {submission.grade != null && (
            <span className="ml-2 text-sm font-semibold text-primary dark:text-white">
              {submission.grade}{t('assignment.points', '점')}
            </span>
          )}
          {submission.feedback && (
            <p className="text-sm text-text-muted mt-2 italic">"{submission.feedback}"</p>
          )}
        </div>
      )}
      <textarea className="input resize-none mb-3" rows={4} value={content}
                onChange={e => setContent(e.target.value)}
                placeholder={t('assignment.contentPlaceholder', '답안을 입력하세요…')} />
      <input type="file" onChange={e => setFile(e.target.files?.[0] || null)}
             className="text-sm text-text-muted file:mr-2 file:px-3 file:py-1.5 file:rounded-full file:border-0 file:bg-primary file:text-white mb-3" />
      <button onClick={submit} disabled={busy} className="btn-hero disabled:opacity-50">
        {submission ? t('assignment.resubmit', '재제출') : t('assignment.submit', '제출')}
      </button>
    </div>
  )
}

function TeacherSubmissionRow({ submission, onChange }) {
  const { t } = useTranslation()
  const [grade, setGrade] = useState(submission.grade ?? '')
  const [feedback, setFeedback] = useState(submission.feedback ?? '')
  const [busy, setBusy] = useState(false)
  const [aiResult, setAiResult] = useState(null)
  const [checking, setChecking] = useState(false)

  const doGrade = async () => {
    if (grade === '') return
    setBusy(true)
    try { await gradeSubmission(submission.id, { grade: +grade, feedback }); onChange() }
    finally { setBusy(false) }
  }
  const doReturn = async () => {
    setBusy(true)
    try { await returnSubmission(submission.id); onChange() }
    finally { setBusy(false) }
  }
  const doAiCheck = async () => {
    setChecking(true)
    try {
      const res = await aiCheckSubmission(submission.id)
      setAiResult(res.data)
      if (res.data?.suggestedScore != null) setGrade(res.data.suggestedScore)
      if (res.data?.feedback) setFeedback(res.data.feedback)
    } catch {
      setAiResult({ feedback: t('assignment.aiCheckFailed', 'AI 점검 실패') })
    } finally { setChecking(false) }
  }

  return (
    <div className="card p-4">
      <div className="flex items-center justify-between mb-2">
        <p className="font-semibold text-on-surface dark:text-white">{submission.studentName}</p>
        <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${STATUS_STYLE[submission.status] || ''}`}>
          {submission.status}
        </span>
      </div>
      {submission.content && (
        <p className="text-sm text-on-surface-variant whitespace-pre-wrap mb-3">{submission.content}</p>
      )}
      {aiResult && (
        <div className="bg-accent-container text-on-accent-container text-sm rounded-lg p-3 mb-3">
          <p className="font-semibold mb-1">{t('assignment.aiSuggestion', 'AI 제안')}: {aiResult.suggestedScore ?? '-'}{t('assignment.points', '점')}</p>
          <p>{aiResult.feedback}</p>
          {aiResult.strengths?.length > 0 && <p className="mt-1 text-xs">✓ {aiResult.strengths.join(', ')}</p>}
          {aiResult.improvements?.length > 0 && <p className="text-xs">△ {aiResult.improvements.join(', ')}</p>}
        </div>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <input type="number" className="input w-24" value={grade} onChange={e => setGrade(e.target.value)}
               placeholder={t('assignment.points', '점')} />
        <input className="input flex-1 min-w-[140px]" value={feedback} onChange={e => setFeedback(e.target.value)}
               placeholder={t('assignment.feedback', '피드백')} />
        <button onClick={doAiCheck} disabled={checking} className="btn-secondary disabled:opacity-50">
          {checking ? '…' : t('assignment.aiCheck', 'AI 점검')}
        </button>
        <button onClick={doGrade} disabled={busy} className="btn-hero disabled:opacity-50">{t('assignment.grade', '채점')}</button>
        <button onClick={doReturn} disabled={busy} className="btn-secondary disabled:opacity-50">{t('assignment.return', '반려')}</button>
      </div>
    </div>
  )
}
