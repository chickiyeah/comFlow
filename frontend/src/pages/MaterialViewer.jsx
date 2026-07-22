import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import {
  getMaterial, getMaterialSummary, materialAi,
  getBookmarks, addBookmark, deleteBookmark,
} from '../api/material'

const SIDE_TABS = [
  { key: 'summary', icon: 'summarize', label: '요약' },
  { key: 'chat',    icon: 'forum',     label: '튜터' },
  { key: 'bookmarks', icon: 'bookmark', label: '북마크' },
]

export default function MaterialViewer() {
  const { materialId } = useParams()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [material, setMaterial] = useState(null)
  const [loading, setLoading] = useState(true)
  const [sideTab, setSideTab] = useState('summary')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getMaterial(materialId)
      setMaterial(res.data)
    } finally { setLoading(false) }
  }, [materialId])

  useEffect(() => { load() }, [load])

  if (loading) return <Layout><p className="text-center text-text-muted py-12">{t('common.loading', '불러오는 중…')}</p></Layout>
  if (!material) return <Layout><p className="text-center text-text-muted py-12">-</p></Layout>

  const isPdf = material.contentType === 'application/pdf'
  const isImage = material.contentType?.startsWith('image/')
  const isVideo = material.contentType?.startsWith('video/')
  const isAudio = material.contentType?.startsWith('audio/')

  return (
    <Layout>
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-2 mb-1">
          <button onClick={() => navigate(`/classroom/${material.classId}`)} className="text-text-muted hover:text-primary">
            <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          </button>
          <h1 className="font-space text-xl font-bold text-primary dark:text-white truncate">{material.title}</h1>
        </div>
        {material.instructions && <p className="text-sm text-text-muted mb-4 pl-7">{material.instructions}</p>}

        <div className="flex flex-wrap gap-5">
          {/* 뷰어 */}
          <div className="flex-1 min-w-[320px]">
            <div className="card p-2 min-h-[500px] flex items-center justify-center overflow-hidden">
              {!material.hasFile ? (
                <p className="text-text-muted py-20">{t('material.noFile', '첨부 파일이 없습니다.')}</p>
              ) : isPdf ? (
                <iframe title={material.filename} src={material.streamUrl} className="w-full h-[75vh] rounded-lg" />
              ) : isImage ? (
                <img src={material.streamUrl} alt={material.filename} className="max-w-full max-h-[75vh] rounded-lg" />
              ) : isVideo ? (
                <video controls src={material.streamUrl} className="w-full max-h-[75vh] rounded-lg" />
              ) : isAudio ? (
                <audio controls src={material.streamUrl} className="w-full" />
              ) : (
                <a href={material.streamUrl} className="btn-primary" target="_blank" rel="noreferrer">
                  {t('material.download', '파일 다운로드')}
                </a>
              )}
            </div>
          </div>

          {/* 사이드 패널 */}
          <div className="w-full lg:w-96">
            <div className="flex gap-1 mb-3 border-b border-outline-variant dark:border-[#33355c]">
              {SIDE_TABS.map(tb => (
                <button key={tb.key} onClick={() => setSideTab(tb.key)}
                  className={`flex items-center gap-1 px-3 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors
                    ${sideTab === tb.key ? 'border-accent text-primary dark:text-white' : 'border-transparent text-text-muted'}`}>
                  <span className="material-symbols-outlined text-[16px]">{tb.icon}</span>
                  {t(`material.tab.${tb.key}`, tb.label)}
                </button>
              ))}
            </div>
            {sideTab === 'summary' && <SummaryPanel materialId={materialId} />}
            {sideTab === 'chat' && <ChatPanel materialId={materialId} />}
            {sideTab === 'bookmarks' && <BookmarksPanel materialId={materialId} />}
          </div>
        </div>
      </div>
    </Layout>
  )
}

function SummaryPanel({ materialId }) {
  const { t } = useTranslation()
  const [summary, setSummary] = useState(null)
  const [level, setLevel] = useState('paragraph')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  const generate = async () => {
    setLoading(true); setError(false)
    try {
      const res = await getMaterialSummary(materialId)
      setSummary(res.data)
    } catch { setError(true) } finally { setLoading(false) }
  }

  const LEVELS = [['short', '한 줄'], ['paragraph', '단락'], ['detailed', '상세']]

  return (
    <div className="card p-4">
      {!summary && !loading && (
        <button onClick={generate} className="btn-hero w-full justify-center">
          <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
          {t('material.generateSummary', 'AI 요약 생성')}
        </button>
      )}
      {loading && <p className="text-center text-text-muted py-6">{t('common.loading', '불러오는 중…')}</p>}
      {error && <p className="text-danger text-sm">{t('material.summaryFailed', '요약 생성 실패')}</p>}
      {summary && (
        <>
          <div className="flex gap-1 mb-3">
            {LEVELS.map(([k, l]) => (
              <button key={k} onClick={() => setLevel(k)}
                className={level === k ? 'chip-active' : 'chip'}>{l}</button>
            ))}
          </div>
          <p className="text-body-md text-on-surface dark:text-[#e6e6f5] whitespace-pre-wrap">
            {level === 'short' ? summary.shortSummary : level === 'paragraph' ? summary.paragraphSummary : summary.detailedSummary}
          </p>
        </>
      )}
    </div>
  )
}

function ChatPanel({ materialId }) {
  const { t } = useTranslation()
  const [level, setLevel] = useState('중급')
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)

  const send = async () => {
    const q = input.trim()
    if (!q || sending) return
    setInput('')
    setMessages(m => [...m, { role: 'user', text: q }])
    setSending(true)
    try {
      const res = await materialAi(materialId, { action: 'chat', message: q, level })
      setMessages(m => [...m, { role: 'assistant', text: res.data?.answer || '' }])
    } catch {
      setMessages(m => [...m, { role: 'assistant', text: t('material.chatFailed', '답변 실패'), error: true }])
    } finally { setSending(false) }
  }

  return (
    <div className="card p-4 flex flex-col h-[420px]">
      <div className="flex gap-1 mb-3">
        {['초급', '중급', '고급'].map(l => (
          <button key={l} onClick={() => setLevel(l)} className={level === l ? 'chip-active' : 'chip'}>{l}</button>
        ))}
      </div>
      <div className="flex-1 overflow-y-auto space-y-2 mb-3">
        {messages.map((m, i) => (
          <div key={i} className={`text-sm px-3 py-2 rounded-lg max-w-[90%] ${
            m.role === 'user' ? 'bg-primary text-on-primary ml-auto' : m.error ? 'bg-danger-bg text-danger-text' : 'bg-surface-container dark:bg-[#25274a]'
          }`}>{m.text}</div>
        ))}
      </div>
      <div className="flex gap-2">
        <input className="input flex-1" value={input} onChange={e => setInput(e.target.value)}
               onKeyDown={e => e.key === 'Enter' && send()}
               placeholder={t('material.askPlaceholder', '자료에 대해 질문하기…')} />
        <button onClick={send} disabled={sending} className="btn-hero disabled:opacity-50">
          <span className="material-symbols-outlined text-[18px]">send</span>
        </button>
      </div>
    </div>
  )
}

function BookmarksPanel({ materialId }) {
  const { t } = useTranslation()
  const [bookmarks, setBookmarks] = useState([])
  const [page, setPage] = useState('')

  const load = useCallback(() => {
    getBookmarks(materialId).then(res => setBookmarks(res.data || [])).catch(() => {})
  }, [materialId])
  useEffect(() => { load() }, [load])

  const add = async () => {
    const p = parseInt(page, 10)
    if (!p || p < 1) return
    await addBookmark(materialId, { page: p })
    setPage('')
    load()
  }
  const remove = async (p) => { await deleteBookmark(materialId, p); load() }

  return (
    <div className="card p-4">
      <div className="flex gap-2 mb-3">
        <input type="number" min={1} className="input flex-1" value={page} onChange={e => setPage(e.target.value)}
               placeholder={t('material.pageNumber', '페이지 번호')} />
        <button onClick={add} className="btn-secondary">{t('material.addBookmark', '추가')}</button>
      </div>
      {bookmarks.length === 0 ? (
        <p className="text-sm text-text-muted">{t('material.noBookmarks', '저장된 페이지가 없습니다.')}</p>
      ) : (
        <div className="space-y-1">
          {bookmarks.map(b => (
            <div key={b.page} className="flex items-center justify-between px-3 py-2 rounded-lg bg-surface-container-low dark:bg-[#1c1e3a]">
              <span className="text-sm font-semibold">{t('material.page', '페이지')} {b.page}</span>
              <button onClick={() => remove(b.page)} className="text-text-muted hover:text-danger">
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
