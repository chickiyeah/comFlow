import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { startInterview, submitAnswer, listSessions, getSessionDetail } from '../api/interview'
import api from '../api/axios'

// ──────────────────────────────────────────────────────────────
// 탭 상수
// ──────────────────────────────────────────────────────────────
const TAB_NEW      = 'new'
const TAB_HISTORY  = 'history'

export default function Interview() {
  const { t } = useTranslation()
  const [tab, setTab] = useState(TAB_NEW)

  return (
    <Layout title="가상 면접">
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-4">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-medium text-primary dark:text-white">가상 면접</h1>
        <span className="chip">AI 면접관</span>
      </div>
      <p className="text-sm text-slate-500 dark:text-slate-400">
        내 포트폴리오를 분석해 실전 면접 질문을 생성합니다. 지원하고 싶은 직장을 선택하거나 직접 입력하세요.
      </p>

      {/* 탭 */}
      <div className="flex gap-2 border-b border-slate-200 dark:border-slate-700 pb-1">
        {[{ key: TAB_NEW, label: '새 면접 시작' }, { key: TAB_HISTORY, label: '면접 기록' }].map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`px-4 py-2 text-sm rounded-t transition-colors ${
              tab === key
                ? 'text-primary font-medium border-b-2 border-primary -mb-px'
                : 'text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === TAB_NEW    && <NewInterviewPanel />}
      {tab === TAB_HISTORY && <HistoryPanel />}
    </div>
    </Layout>
  )
}

// ──────────────────────────────────────────────────────────────
// 새 면접 시작 패널
// ──────────────────────────────────────────────────────────────
function NewInterviewPanel() {
  const [phase, setPhase]         = useState('setup')   // setup | interviewing | result
  const [savedJobs, setSavedJobs] = useState([])
  const [selectedJob, setSelectedJob] = useState(null)
  const [manualCompany, setManualCompany]   = useState('')
  const [manualPosition, setManualPosition] = useState('')
  const [totalQuestions, setTotalQuestions] = useState(5)
  const [loading, setLoading] = useState(false)
  const [session, setSession] = useState(null)     // { sessionId, company, position, totalQuestions }
  const [messages, setMessages] = useState([])     // [{ role: 'ai'|'user'|'feedback', content }]
  const [currentIndex, setCurrentIndex] = useState(0)
  const [inputText, setInputText] = useState('')
  const [result, setResult] = useState(null)       // SessionDetail
  const bottomRef = useRef(null)

  useEffect(() => {
    api.get('/career/saved-jobs').then(r => {
      setSavedJobs(r.data || [])
    }).catch(() => {})
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const handleStart = async () => {
    const company  = selectedJob ? selectedJob.company  : manualCompany.trim()
    const position = selectedJob ? selectedJob.title    : manualPosition.trim()
    if (!company || !position) return alert('회사명과 직무를 입력해 주세요.')

    setLoading(true)
    try {
      const res = await startInterview({
        savedJobId: selectedJob?.id ?? null,
        company,
        position,
        totalQuestions,
      })
      const data = res.data
      setSession(data)
      setMessages([{ role: 'ai', content: data.question }])
      setCurrentIndex(0)
      setPhase('interviewing')
    } catch (e) {
      alert('면접 시작에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmitAnswer = async () => {
    const answer = inputText.trim()
    if (!answer) return
    setInputText('')
    setMessages(prev => [...prev, { role: 'user', content: answer }])
    setLoading(true)
    try {
      const res = await submitAnswer(session.sessionId, answer)
      const data = res.data

      // 피드백 추가
      setMessages(prev => [...prev, { role: 'feedback', content: data.feedback }])

      if (data.finished) {
        // 종합 피드백 + 결과 조회
        setMessages(prev => [...prev, {
          role: 'overall',
          content: data.overallFeedback
        }])
        const detail = await getSessionDetail(session.sessionId)
        setResult(detail.data)
        setPhase('result')
      } else {
        // 다음 질문
        setCurrentIndex(data.currentIndex)
        setMessages(prev => [...prev, { role: 'ai', content: data.nextQuestion }])
      }
    } catch (e) {
      setMessages(prev => [...prev, { role: 'feedback', content: '답변 처리 중 오류가 발생했습니다.' }])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmitAnswer()
    }
  }

  const handleReset = () => {
    setPhase('setup')
    setSession(null)
    setMessages([])
    setCurrentIndex(0)
    setResult(null)
    setSelectedJob(null)
    setManualCompany('')
    setManualPosition('')
  }

  // ── 설정 화면 ──
  if (phase === 'setup') {
    return (
      <div className="space-y-5">
        {/* 저장된 공고 선택 */}
        {savedJobs.length > 0 && (
          <section>
            <h2 className="text-sm font-medium text-slate-600 dark:text-slate-300 mb-2">
              저장된 채용공고에서 선택
            </h2>
            <div className="grid gap-2 max-h-52 overflow-y-auto pr-1">
              {savedJobs.map(job => (
                <button
                  key={job.id}
                  onClick={() => setSelectedJob(selectedJob?.id === job.id ? null : job)}
                  className={`text-left px-4 py-3 rounded-lg border transition-colors text-sm ${
                    selectedJob?.id === job.id
                      ? 'border-primary bg-primary/5 dark:bg-primary/10 text-primary font-medium'
                      : 'border-slate-200 dark:border-slate-700 hover:border-primary/50'
                  }`}
                >
                  <span className="font-medium">{job.company}</span>
                  <span className="text-slate-400 mx-2">·</span>
                  <span className="text-slate-600 dark:text-slate-300">{job.title}</span>
                </button>
              ))}
            </div>
          </section>
        )}

        <div className="flex items-center gap-3 text-sm text-slate-400">
          <div className="flex-1 h-px bg-slate-200 dark:bg-slate-700" />
          <span>{savedJobs.length > 0 ? '또는 직접 입력' : '직접 입력'}</span>
          <div className="flex-1 h-px bg-slate-200 dark:bg-slate-700" />
        </div>

        {/* 직접 입력 */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs text-slate-500 mb-1 block">회사명</label>
            <input
              className="input w-full"
              placeholder="예: 카카오"
              value={selectedJob ? selectedJob.company : manualCompany}
              onChange={e => { setSelectedJob(null); setManualCompany(e.target.value) }}
            />
          </div>
          <div>
            <label className="text-xs text-slate-500 mb-1 block">직무 / 공고 제목</label>
            <input
              className="input w-full"
              placeholder="예: 백엔드 개발자"
              value={selectedJob ? selectedJob.title : manualPosition}
              onChange={e => { setSelectedJob(null); setManualPosition(e.target.value) }}
            />
          </div>
        </div>

        {/* 질문 수 선택 */}
        <div>
          <label className="text-xs text-slate-500 mb-2 block">질문 수</label>
          <div className="flex gap-2">
            {[3, 5, 7, 10].map(n => (
              <button
                key={n}
                onClick={() => setTotalQuestions(n)}
                className={`px-4 py-2 rounded-lg text-sm border transition-colors ${
                  totalQuestions === n
                    ? 'bg-primary text-white border-primary'
                    : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-primary/50'
                }`}
              >
                {n}개
              </button>
            ))}
          </div>
        </div>

        <button
          onClick={handleStart}
          disabled={loading}
          className="btn-primary w-full"
        >
          {loading ? '포트폴리오 분석 중…' : '면접 시작'}
        </button>
      </div>
    )
  }

  // ── 면접 진행 화면 ──
  if (phase === 'interviewing') {
    const progress = Math.round((currentIndex / session.totalQuestions) * 100)

    return (
      <div className="space-y-4">
        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <div>
            <span className="font-medium text-primary">{session.company}</span>
            <span className="text-slate-400 mx-2">·</span>
            <span className="text-sm text-slate-500">{session.position}</span>
          </div>
          <span className="chip text-xs">
            {currentIndex + 1} / {session.totalQuestions}
          </span>
        </div>

        {/* 진행 바 */}
        <div className="h-1.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
          <div
            className="h-full bg-primary rounded-full transition-all duration-500"
            style={{ width: `${progress}%` }}
          />
        </div>

        {/* 채팅 영역 */}
        <div className="space-y-3 max-h-[440px] overflow-y-auto pr-1">
          {messages.map((msg, i) => (
            <ChatBubble key={i} msg={msg} />
          ))}
          {loading && (
            <div className="flex gap-2 items-start">
              <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white text-xs shrink-0">AI</div>
              <div className="card px-4 py-3 text-sm text-slate-400 flex gap-1 items-center">
                <span className="animate-bounce">●</span>
                <span className="animate-bounce delay-100">●</span>
                <span className="animate-bounce delay-200">●</span>
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* 입력창 */}
        <div className="flex gap-2 items-end">
          <textarea
            className="input flex-1 resize-none text-sm"
            rows={3}
            placeholder="답변을 입력하세요… (Shift+Enter 줄바꿈, Enter 제출)"
            value={inputText}
            onChange={e => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={loading}
          />
          <button
            onClick={handleSubmitAnswer}
            disabled={loading || !inputText.trim()}
            className="btn-primary px-5 py-3 shrink-0"
          >
            제출
          </button>
        </div>
      </div>
    )
  }

  // ── 결과 화면 ──
  if (phase === 'result' && result) {
    return (
      <div className="space-y-5">
        <div className="card p-5 border-l-4 border-secondary-fixed">
          <h2 className="font-medium text-primary mb-2">종합 피드백</h2>
          <p className="text-sm leading-relaxed text-slate-600 dark:text-slate-300 whitespace-pre-wrap">
            {result.overallFeedback}
          </p>
        </div>

        <div className="space-y-3">
          {result.questions.map((q, i) => (
            <div key={i} className="card p-4 space-y-2">
              <p className="text-xs text-slate-400 font-medium">Q{i + 1}</p>
              <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{q.question}</p>
              <p className="text-sm text-slate-600 dark:text-slate-400 border-l-2 border-slate-200 pl-3">
                {q.answer || '(미답변)'}
              </p>
              {q.feedback && (
                <div className="bg-blue-50 dark:bg-blue-950/30 rounded-lg px-3 py-2 text-xs text-blue-700 dark:text-blue-300 leading-relaxed">
                  {q.feedback}
                </div>
              )}
            </div>
          ))}
        </div>

        <button onClick={handleReset} className="btn-secondary w-full">
          새 면접 시작
        </button>
      </div>
    )
  }

  return null
}

// ──────────────────────────────────────────────────────────────
// 채팅 버블 컴포넌트
// ──────────────────────────────────────────────────────────────
function ChatBubble({ msg }) {
  if (msg.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[78%] bg-primary text-white rounded-2xl rounded-tr-sm px-4 py-3 text-sm leading-relaxed">
          {msg.content}
        </div>
      </div>
    )
  }
  if (msg.role === 'feedback') {
    return (
      <div className="mx-2">
        <div className="bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-xl px-4 py-3 text-xs text-amber-800 dark:text-amber-300 leading-relaxed">
          <span className="font-medium block mb-1">피드백</span>
          {msg.content}
        </div>
      </div>
    )
  }
  if (msg.role === 'overall') {
    return (
      <div className="mx-2">
        <div className="bg-secondary-fixed/20 border border-secondary-fixed/40 rounded-xl px-4 py-3 text-sm text-slate-700 dark:text-slate-200 leading-relaxed">
          <span className="font-medium block mb-1 text-primary">종합 평가</span>
          {msg.content}
        </div>
      </div>
    )
  }
  // AI 질문
  return (
    <div className="flex gap-2 items-start">
      <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white text-xs shrink-0 mt-0.5">AI</div>
      <div className="max-w-[78%] card px-4 py-3 text-sm leading-relaxed text-slate-700 dark:text-slate-200 rounded-2xl rounded-tl-sm">
        {msg.content}
      </div>
    </div>
  )
}

// ──────────────────────────────────────────────────────────────
// 면접 기록 패널
// ──────────────────────────────────────────────────────────────
function HistoryPanel() {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading]   = useState(true)
  const [selected, setSelected] = useState(null)  // SessionDetail

  useEffect(() => {
    listSessions().then(r => {
      setSessions(r.data || [])
    }).finally(() => setLoading(false))
  }, [])

  const handleSelect = async (sessionId) => {
    if (selected?.sessionId === sessionId) { setSelected(null); return }
    const r = await getSessionDetail(sessionId)
    setSelected(r.data)
  }

  if (loading) return <p className="text-sm text-slate-400 py-4">불러오는 중…</p>
  if (sessions.length === 0) return (
    <p className="text-sm text-slate-400 py-4 text-center">아직 면접 기록이 없습니다.</p>
  )

  return (
    <div className="space-y-3">
      {sessions.map(s => (
        <div key={s.sessionId}>
          <button
            onClick={() => handleSelect(s.sessionId)}
            className="w-full text-left card p-4 hover:border-primary/40 transition-colors"
          >
            <div className="flex items-center justify-between">
              <div>
                <span className="font-medium text-sm text-slate-700 dark:text-slate-200">{s.company}</span>
                <span className="text-slate-400 mx-2">·</span>
                <span className="text-sm text-slate-500">{s.position}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`chip text-xs ${
                  s.status === 'FINISHED' ? 'chip-active' : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
                }`}>
                  {s.status === 'FINISHED' ? '완료' : '진행 중'}
                </span>
                <span className="text-xs text-slate-400">{s.currentIndex}/{s.totalQuestions}</span>
              </div>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              {new Date(s.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })}
            </p>
          </button>

          {/* 상세 결과 아코디언 */}
          {selected?.sessionId === s.sessionId && (
            <div className="mt-1 space-y-3 px-1">
              {selected.overallFeedback && (
                <div className="card p-4 border-l-4 border-secondary-fixed">
                  <p className="text-xs font-medium text-primary mb-1">종합 피드백</p>
                  <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                    {selected.overallFeedback}
                  </p>
                </div>
              )}
              {selected.questions.map((q, i) => (
                <div key={i} className="card p-3 space-y-1.5 text-sm">
                  <p className="text-xs text-slate-400">Q{i + 1}</p>
                  <p className="font-medium text-slate-700 dark:text-slate-200">{q.question}</p>
                  {q.answer && (
                    <p className="text-slate-500 dark:text-slate-400 border-l-2 border-slate-200 pl-2 text-xs">
                      {q.answer}
                    </p>
                  )}
                  {q.feedback && (
                    <div className="bg-blue-50 dark:bg-blue-950/30 rounded-lg px-3 py-2 text-xs text-blue-700 dark:text-blue-300">
                      {q.feedback}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
