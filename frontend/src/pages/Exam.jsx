import { useState, useEffect, useRef, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { generateKmateQuiz, checkKmateQuiz } from '../api/kmate'

const DURATION_PER_Q = 60 // 문항당 60초

export default function Exam() {
  const { t } = useTranslation()
  const [stage, setStage] = useState('setup') // setup | loading | running | result
  const [topic, setTopic] = useState('')
  const [count, setCount] = useState(5)
  const [questions, setQuestions] = useState([])
  const [current, setCurrent] = useState(0)
  const [answers, setAnswers] = useState({})
  const [timeLeft, setTimeLeft] = useState(0)
  const [result, setResult] = useState(null)
  const timerRef = useRef(null)

  const start = async () => {
    if (!topic.trim()) return
    setStage('loading')
    try {
      const res = await generateKmateQuiz({ topic, count, language: '한국어' })
      const qs = res.data || []
      if (qs.length === 0) throw new Error('empty')
      setQuestions(qs)
      setAnswers({})
      setCurrent(0)
      setTimeLeft(qs.length * DURATION_PER_Q)
      setStage('running')
    } catch {
      alert(t('exam.generateFailed', '퀴즈 생성에 실패했습니다.'))
      setStage('setup')
    }
  }

  const finish = useCallback(async () => {
    clearInterval(timerRef.current)
    setStage('loading')
    const safeItems = questions.map((q, i) => ({
      question: q.text, correctAnswer: q.correctAnswer, userAnswer: answers[i] ?? '',
    }))
    try {
      const res = await checkKmateQuiz(safeItems)
      setResult(res.data)
    } catch {
      setResult(null)
    } finally {
      setStage('result')
    }
  }, [questions, answers])

  useEffect(() => {
    if (stage !== 'running') return
    timerRef.current = setInterval(() => {
      setTimeLeft(t => {
        if (t <= 1) { finish(); return 0 }
        return t - 1
      })
    }, 1000)
    return () => clearInterval(timerRef.current)
  }, [stage, finish])

  const selectAnswer = (idx) => setAnswers(a => ({ ...a, [current]: String(idx) }))
  const mm = Math.floor(timeLeft / 60), ss = timeLeft % 60

  const reset = () => { setStage('setup'); setQuestions([]); setResult(null) }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto">
        <h1 className="font-space text-2xl font-bold text-primary dark:text-white flex items-center gap-2 mb-6">
          <span className="material-symbols-outlined text-accent">timer</span>
          {t('exam.title', 'TOPIK 모의고사')}
        </h1>

        {stage === 'setup' && (
          <div className="card p-6 space-y-4">
            <input className="input" value={topic} onChange={e => setTopic(e.target.value)}
                   placeholder={t('exam.topicPlaceholder', '주제 (예: 문법, 어휘, 읽기)')} />
            <div className="flex items-center gap-3">
              <label className="text-sm text-text-muted">{t('exam.count', '문항 수')}</label>
              <input type="number" min={3} max={10} value={count}
                     onChange={e => setCount(+e.target.value)} className="input w-24" />
            </div>
            <button onClick={start} className="btn-hero w-full justify-center">
              {t('exam.start', '시작')}
            </button>
          </div>
        )}

        {stage === 'loading' && (
          <div className="card p-10 text-center text-text-muted">
            <div className="w-8 h-8 border-4 border-primary border-t-accent rounded-full animate-spin mx-auto mb-4" />
            {t('common.loading', '불러오는 중…')}
          </div>
        )}

        {stage === 'running' && questions[current] && (
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="chip">{current + 1} / {questions.length}</span>
              <span className={`chip-active ${timeLeft < 30 ? 'bg-danger text-white shadow-none' : ''}`}>
                {mm}:{String(ss).padStart(2, '0')}
              </span>
            </div>
            <div className="card p-6">
              <p className="text-title-lg text-on-surface dark:text-white mb-5">{questions[current].text}</p>
              <div className="space-y-2">
                {(questions[current].options || []).map((opt, i) => (
                  <button key={i} onClick={() => selectAnswer(i)}
                    className={`w-full text-left px-4 py-3 rounded-lg border transition-colors
                      ${answers[current] === String(i)
                        ? 'border-accent bg-accent-container text-on-accent-container'
                        : 'border-outline-variant dark:border-[#33355c] hover:bg-surface-container dark:hover:bg-[#25274a]'}`}>
                    {opt}
                  </button>
                ))}
              </div>
              <div className="flex justify-between mt-6">
                <button onClick={() => setCurrent(c => Math.max(0, c - 1))} disabled={current === 0}
                        className="btn-secondary disabled:opacity-40">{t('exam.prev', '이전')}</button>
                {current === questions.length - 1 ? (
                  <button onClick={finish} className="btn-hero">{t('exam.submit', '제출')}</button>
                ) : (
                  <button onClick={() => setCurrent(c => c + 1)} className="btn-primary">{t('exam.next', '다음')}</button>
                )}
              </div>
            </div>
          </div>
        )}

        {stage === 'result' && (
          <div className="card p-6 text-center">
            {result ? (
              <>
                <p className="text-display-lg text-primary dark:text-white">{result.score} / {result.total}</p>
                <p className="text-text-muted mt-2 mb-6">{t('exam.resultLabel', '정답 개수')}</p>
                <div className="space-y-2 text-left">
                  {result.results?.map((r, i) => (
                    <div key={i} className={`p-3 rounded-lg text-sm ${r.correct ? 'bg-success-bg text-success-text' : 'bg-danger-bg text-danger-text'}`}>
                      <p className="font-semibold">{r.question}</p>
                      <p>{t('exam.yourAnswer', '내 답')}: {r.userAnswer || '-'} {!r.correct && `(${t('exam.correctAnswer', '정답')}: ${r.correctAnswer})`}</p>
                    </div>
                  ))}
                </div>
              </>
            ) : <p className="text-text-muted">{t('exam.checkFailed', '채점에 실패했습니다.')}</p>}
            <button onClick={reset} className="btn-secondary mt-6">{t('exam.retry', '다시 하기')}</button>
          </div>
        )}
      </div>
    </Layout>
  )
}
