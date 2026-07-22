import { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { askKmate, getKmateHistory } from '../api/kmate'

export default function KMate() {
  const { t } = useTranslation()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const endRef = useRef(null)

  useEffect(() => {
    getKmateHistory().then(res => {
      const hist = (res.data || []).slice().reverse()
      const msgs = []
      hist.forEach(h => {
        msgs.push({ role: 'user', text: h.question })
        msgs.push({ role: 'assistant', text: h.answer })
      })
      setMessages(msgs)
    }).catch(() => {})
  }, [])

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, sending])

  const send = async () => {
    const q = input.trim()
    if (!q || sending) return
    setInput('')
    setMessages(m => [...m, { role: 'user', text: q }])
    setSending(true)
    try {
      const res = await askKmate(q)
      setMessages(m => [...m, { role: 'assistant', text: res.data?.answer || '' }])
    } catch {
      setMessages(m => [...m, { role: 'assistant', text: t('kmate.error', '답변 생성에 실패했습니다.'), error: true }])
    } finally { setSending(false) }
  }

  return (
    <Layout>
      <div className="max-w-3xl mx-auto flex flex-col h-[calc(100vh-8rem)]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-accent text-3xl">robot_2</span>
            <div>
              <h1 className="font-space text-2xl font-bold text-primary dark:text-white">K.MATE</h1>
              <p className="text-sm text-text-muted">{t('kmate.subtitle', 'TOPIK 한국어 AI 튜터')}</p>
            </div>
          </div>
          <Link to="/exam" className="btn-secondary">
            <span className="material-symbols-outlined text-[18px]">timer</span>
            {t('kmate.examMode', '모의고사')}
          </Link>
        </div>

        <div className="card flex-1 overflow-y-auto p-4 space-y-4">
          {messages.length === 0 && !sending && (
            <div className="h-full flex flex-col items-center justify-center text-text-muted gap-2">
              <span className="material-symbols-outlined text-5xl text-accent/60">forum</span>
              <p>{t('kmate.placeholder', '한국어 문법·어휘를 물어보세요.')}</p>
            </div>
          )}
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[80%] px-4 py-2.5 rounded-2xl text-body-md whitespace-pre-wrap
                ${m.role === 'user'
                  ? 'bg-primary text-on-primary rounded-br-sm'
                  : m.error
                    ? 'bg-danger-bg text-danger-text rounded-bl-sm'
                    : 'bg-surface-container text-on-surface dark:bg-[#25274a] dark:text-[#e6e6f5] rounded-bl-sm'}`}>
                {m.text}
              </div>
            </div>
          ))}
          {sending && (
            <div className="flex justify-start">
              <div className="px-4 py-3 rounded-2xl bg-surface-container dark:bg-[#25274a]">
                <span className="inline-block w-2 h-2 bg-accent rounded-full animate-pulse" />
              </div>
            </div>
          )}
          <div ref={endRef} />
        </div>

        <div className="flex gap-2 mt-4">
          <input className="input flex-1" value={input} onChange={e => setInput(e.target.value)}
                 onKeyDown={e => e.key === 'Enter' && send()}
                 placeholder={t('kmate.inputPlaceholder', '질문을 입력하세요…')} />
          <button onClick={send} disabled={sending} className="btn-hero disabled:opacity-50">
            <span className="material-symbols-outlined text-[20px]">send</span>
          </button>
        </div>
      </div>
    </Layout>
  )
}
