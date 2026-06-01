import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import useAuthStore from '../store/authStore'
import useThemeStore from '../store/themeStore'
import { askKomjeong } from '../api/komjeong'
import { listSessions, createSession, getSession, sendMessage, renameSession, deleteSession } from '../api/chat'
import {
  pruneAndList, createGuestSession, getGuestSession,
  appendGuestMessages, renameGuestSession, deleteGuestSession,
} from '../utils/guestChat'

const SUGGESTIONS = [
  '오늘 학식 알려줘',
  '컴퓨터정보과 졸업요건 알려줘',
  '정보처리기사 시험일정',
  '백엔드 개발자 로드맵',
]

export default function Landing() {
  const navigate = useNavigate()
  const token = useAuthStore(s => s.token)
  const user  = useAuthStore(s => s.user)
  const logout = useAuthStore(s => s.logout)
  const dark = useThemeStore(s => s.dark)
  const toggle = useThemeStore(s => s.toggle)

  const [query, setQuery]     = useState('')
  const [loading, setLoading] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const inputRef = useRef(null)
  const scrollRef = useRef(null)

  const [sessions, setSessions]     = useState([])
  const [activeId, setActiveId]     = useState(null)
  const [activeKey, setActiveKey]   = useState(null)  // 비로그인: 컴정이 sessionKey
  const [activeTitle, setActiveTitle] = useState('')
  const [messages, setMessages]     = useState([])

  // 인라인 이름 편집
  const [editingId, setEditingId] = useState(null)
  const [editingValue, setEditingValue] = useState('')
  const editInputRef = useRef(null)
  useEffect(() => { if (editingId) editInputRef.current?.focus() }, [editingId])

  useEffect(() => { inputRef.current?.focus() }, [activeId])

  useEffect(() => {
    if (token) {
      listSessions().then(r => setSessions(r.data ?? [])).catch(() => setSessions([]))
    } else {
      setSessions(pruneAndList())
    }
  }, [token])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  // ── 공통 액션 ──────────────────────────────────────────────
  const newChat = () => {
    setActiveId(null); setActiveKey(null); setMessages([]); setActiveTitle('')
    setQuery(''); setSidebarOpen(false); inputRef.current?.focus()
  }

  const openSession = async (id) => {
    setSidebarOpen(false); setMessages([]); setLoading(true)
    try {
      if (token) {
        const r = await getSession(id)
        setActiveId(id)
        setActiveTitle(r.data?.title ?? '')
        setMessages(r.data?.messages ?? [])
      } else {
        const s = getGuestSession(id)
        if (s) {
          setActiveId(id); setActiveKey(s.sessionKey)
          setActiveTitle(s.title); setMessages(s.messages ?? [])
        }
      }
    } finally { setLoading(false) }
  }

  const handleDelete = async (id, e) => {
    e.stopPropagation()
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return
    try {
      if (token) await deleteSession(id)
      else       deleteGuestSession(id)
      setSessions(prev => prev.filter(s => s.id !== id))
      if (activeId === id) newChat()
    } catch {}
  }

  const startEdit = (s, e) => {
    e.stopPropagation()
    setEditingId(s.id); setEditingValue(s.title || '')
  }

  const cancelEdit = () => { setEditingId(null); setEditingValue('') }

  const commitEdit = async () => {
    const id = editingId
    const value = editingValue.trim()
    if (!id || !value) { cancelEdit(); return }
    try {
      if (token) {
        const r = await renameSession(id, value)
        const newTitle = r.data?.title ?? value
        setSessions(prev => prev.map(s => s.id === id ? { ...s, title: newTitle } : s))
        if (activeId === id) setActiveTitle(newTitle)
      } else {
        const updated = renameGuestSession(id, value)
        const newTitle = updated?.title ?? value
        setSessions(prev => prev.map(s => s.id === id ? { ...s, title: newTitle } : s))
        if (activeId === id) setActiveTitle(newTitle)
      }
    } catch { alert('이름 변경 실패') }
    finally { cancelEdit() }
  }

  const handleSend = async (q) => {
    const text = (q ?? query).trim()
    if (!text || loading) return
    setQuery('')

    const tempUser = { id: `tmp-${Date.now()}`, role: 'user', content: text }
    setMessages(prev => [...prev, tempUser])
    setLoading(true)

    try {
      if (token) {
        let r
        if (activeId) r = await sendMessage(activeId, text)
        else          r = await createSession(text)
        if (!activeId) { setActiveId(r.data.sessionId); setActiveTitle(r.data.title) }
        setMessages(prev => [
          ...prev.filter(m => m.id !== tempUser.id),
          r.data.userMessage, r.data.assistantMessage,
        ])
        listSessions().then(res => setSessions(res.data ?? [])).catch(() => {})
      } else {
        // 비로그인 — localStorage + 컴정이 직접 호출
        let session = activeId ? getGuestSession(activeId) : null
        if (!session) {
          session = createGuestSession(text)
          setActiveId(session.id); setActiveKey(session.sessionKey); setActiveTitle(session.title)
        }
        const r = await askKomjeong(text, session.sessionKey)
        const answer = r.data?.answer || '답변을 가져오지 못했습니다.'
        const updated = appendGuestMessages(session.id, text, answer)
        setMessages(updated?.messages ?? [])
        setSessions(pruneAndList())
      }
    } catch {
      setMessages(prev => [
        ...prev.filter(m => m.id !== tempUser.id),
        tempUser,
        { id: `err-${Date.now()}`, role: 'assistant', content: '⚠️ 답변을 가져오지 못했습니다.' },
      ])
    } finally { setLoading(false) }
  }

  // ── UI ────────────────────────────────────────────────────
  return (
    <div className="h-screen flex bg-white dark:bg-[#0b1020] text-on-surface dark:text-slate-100 overflow-hidden">
      {/* 사이드바 백드롭 (모바일) */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/40 z-30 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      {/* 사이드바 */}
      <aside className={`fixed lg:static inset-y-0 left-0 w-64 bg-slate-50 dark:bg-[#0a0f1f] border-r border-slate-100 dark:border-slate-800 flex flex-col z-40 transition-transform
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="p-3 border-b border-slate-100 dark:border-slate-800">
          <button onClick={newChat}
            className="w-full flex items-center justify-center gap-2 py-2.5 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl text-sm font-bold text-primary dark:text-white hover:shadow-sm transition-all">
            <span className="material-symbols-outlined text-[18px]">add</span>새 채팅
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
          {sessions.length === 0 ? (
            <p className="text-xs text-outline dark:text-slate-500 text-center py-8">대화가 없습니다</p>
          ) : sessions.map(s => {
            const isEditing = editingId === s.id
            return (
              <div key={s.id}
                onClick={() => !isEditing && openSession(s.id)}
                onDoubleClick={(e) => startEdit(s, e)}
                className={`w-full px-3 py-2.5 rounded-lg text-sm flex items-center gap-2 group transition-colors cursor-pointer
                  ${activeId === s.id
                    ? 'bg-primary/10 dark:bg-slate-800 text-primary dark:text-white font-medium'
                    : 'text-on-surface-variant dark:text-slate-300 hover:bg-white dark:hover:bg-slate-800/50'}`}>
                <span className="material-symbols-outlined text-[16px] shrink-0 opacity-60">chat_bubble</span>
                {isEditing ? (
                  <input
                    ref={editInputRef}
                    value={editingValue}
                    onChange={(e) => setEditingValue(e.target.value)}
                    onClick={(e) => e.stopPropagation()}
                    onBlur={commitEdit}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') { e.preventDefault(); commitEdit() }
                      if (e.key === 'Escape') { e.preventDefault(); cancelEdit() }
                    }}
                    maxLength={200}
                    className="flex-1 bg-white dark:bg-slate-700 border border-secondary-fixed dark:border-secondary-fixed rounded px-1.5 py-0.5 text-sm focus:outline-none dark:text-white" />
                ) : (
                  <span className="flex-1 truncate">{s.title || '새 채팅'}</span>
                )}
                {!isEditing && (
                  <>
                    <span onClick={(e) => startEdit(s, e)}
                      className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-primary/10 dark:hover:bg-slate-700 text-outline dark:text-slate-400 transition-opacity"
                      title="이름 변경">
                      <span className="material-symbols-outlined text-[14px]">edit</span>
                    </span>
                    <span onClick={(e) => handleDelete(s.id, e)}
                      className="opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-error/10 text-error transition-opacity"
                      title="삭제">
                      <span className="material-symbols-outlined text-[14px]">delete</span>
                    </span>
                  </>
                )}
              </div>
            )
          })}
        </div>
        {!token && (
          <div className="p-3 border-t border-slate-100 dark:border-slate-800 text-xs text-outline dark:text-slate-500">
            <p className="mb-2">대화는 이 기기에 7일간 보관됩니다.</p>
            <button onClick={() => navigate('/login')} className="text-primary dark:text-secondary-fixed font-bold hover:underline">
              로그인하고 영구 저장
            </button>
          </div>
        )}
      </aside>

      {/* 메인 영역 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 상단 바 */}
        <header className="flex items-center justify-between px-4 sm:px-6 py-3 border-b border-slate-100 dark:border-slate-800 bg-white/80 dark:bg-[#0b1020]/80 backdrop-blur sticky top-0 z-30">
          <div className="flex items-center gap-2 min-w-0">
            <button onClick={() => setSidebarOpen(o => !o)} className="lg:hidden p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
              <span className="material-symbols-outlined text-[20px] text-slate-700 dark:text-slate-300">menu</span>
            </button>
            <button onClick={newChat} className="flex items-center gap-2 hover:opacity-80 transition-opacity">
              <span className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">CF</span>
              <span className="text-primary dark:text-white font-bold text-base font-['Space_Grotesk'] hidden sm:inline">CampusFlow</span>
            </button>
            {activeTitle && <span className="hidden md:inline text-sm text-outline dark:text-slate-500 ml-3 truncate max-w-[280px]">— {activeTitle}</span>}
          </div>

          <div className="flex items-center gap-2">
            <button onClick={toggle} title="테마"
              className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300">
              <span className="material-symbols-outlined text-[20px]">{dark ? 'light_mode' : 'dark_mode'}</span>
            </button>
            {token ? (
              <div className="relative">
                <button onClick={() => setMenuOpen(o => !o)}
                  className="flex items-center gap-2 pl-2 pr-3 py-1.5 rounded-full bg-primary/5 dark:bg-slate-800 hover:bg-primary/10 dark:hover:bg-slate-700 transition-colors">
                  <span className="w-7 h-7 rounded-full bg-secondary-fixed text-primary text-xs font-bold flex items-center justify-center">
                    {user?.name?.[0] ?? '학'}
                  </span>
                  <span className="text-sm font-medium hidden sm:block dark:text-white">{user?.name ?? '학우님'}</span>
                </button>
                {menuOpen && (
                  <div className="absolute right-0 mt-2 w-44 bg-white dark:bg-slate-900 rounded-xl shadow-xl border border-slate-100 dark:border-slate-800 overflow-hidden z-50">
                    <button onClick={() => navigate('/dashboard')} className="w-full text-left px-4 py-2.5 text-sm hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center gap-2 dark:text-slate-200">
                      <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">dashboard</span>대시보드
                    </button>
                    <button onClick={() => navigate('/profile')} className="w-full text-left px-4 py-2.5 text-sm hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center gap-2 dark:text-slate-200">
                      <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">manage_accounts</span>프로필
                    </button>
                    <button onClick={() => { logout(); setMenuOpen(false) }} className="w-full text-left px-4 py-2.5 text-sm hover:bg-slate-50 dark:hover:bg-slate-800 flex items-center gap-2 text-error border-t border-slate-100 dark:border-slate-800">
                      <span className="material-symbols-outlined text-[18px]">logout</span>로그아웃
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <>
                <button onClick={() => navigate('/login')}
                  className="px-4 py-1.5 text-sm font-bold text-primary dark:text-white hover:bg-primary/5 dark:hover:bg-slate-800 rounded-full transition-colors">
                  로그인
                </button>
                <button onClick={() => navigate('/register')}
                  className="px-4 py-1.5 text-sm font-bold bg-primary dark:bg-secondary-fixed text-white dark:text-primary rounded-full hover:scale-[1.02] active:scale-95 transition-transform shadow-sm">
                  회원가입
                </button>
              </>
            )}
          </div>
        </header>

        {/* 메시지 영역 */}
        <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 sm:px-6">
          {messages.length === 0 && !activeId ? (
            <div className="h-full flex flex-col items-center justify-center -mt-8">
              <h1 className="font-['Space_Grotesk'] text-4xl sm:text-5xl font-black mb-3 text-center">
                <span className="bg-gradient-to-r from-primary via-secondary-fixed to-primary dark:from-secondary-fixed dark:via-white dark:to-secondary-fixed bg-clip-text text-transparent">
                  컴정이
                </span>
              </h1>
              <p className="text-sm text-on-surface-variant dark:text-slate-400 mb-8">무엇을 도와드릴까요?</p>
              <div className="flex flex-wrap justify-center gap-2 max-w-xl">
                {SUGGESTIONS.map(s => (
                  <button key={s} onClick={() => handleSend(s)}
                    className="px-3.5 py-1.5 text-xs font-medium bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-full border border-slate-200 dark:border-slate-700 transition-colors">
                    {s}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="max-w-3xl mx-auto py-6 space-y-6">
              {messages.map(m => (
                <div key={m.id} className={`flex gap-3 ${m.role === 'user' ? 'justify-end' : ''}`}>
                  {m.role === 'assistant' && (
                    <span className="w-8 h-8 rounded-full bg-secondary-fixed text-primary text-xs font-bold flex items-center justify-center shrink-0">컴</span>
                  )}
                  <div className={`max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap
                    ${m.role === 'user'
                      ? 'bg-primary dark:bg-primary-container text-white rounded-tr-sm'
                      : 'bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800 text-on-surface dark:text-slate-200 rounded-tl-sm'}`}>
                    {m.content}
                  </div>
                  {m.role === 'user' && (
                    <span className="w-8 h-8 rounded-full bg-primary/10 dark:bg-slate-700 text-primary dark:text-white text-xs font-bold flex items-center justify-center shrink-0">
                      {token ? (user?.name?.[0] ?? '나') : '나'}
                    </span>
                  )}
                </div>
              ))}
              {loading && (
                <div className="flex gap-3">
                  <span className="w-8 h-8 rounded-full bg-secondary-fixed text-primary text-xs font-bold flex items-center justify-center shrink-0">컴</span>
                  <div className="px-4 py-3 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-800">
                    <div className="flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 bg-outline dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                      <span className="w-1.5 h-1.5 bg-outline dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                      <span className="w-1.5 h-1.5 bg-outline dark:bg-slate-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 입력창 */}
        <div className="border-t border-slate-100 dark:border-slate-800 bg-white dark:bg-[#0b1020] px-4 sm:px-6 py-4">
          <form onSubmit={e => { e.preventDefault(); handleSend() }} className="max-w-3xl mx-auto">
            <div className="relative">
              <input ref={inputRef} value={query} onChange={e => setQuery(e.target.value)}
                placeholder={activeId ? '메시지 입력' : '컴정이에게 무엇이든 물어보세요'} disabled={loading}
                className="w-full pl-5 pr-14 py-4 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-full focus:border-secondary-fixed focus:outline-none transition-all dark:text-white disabled:opacity-60" />
              <button type="submit" disabled={!query.trim() || loading}
                className="absolute right-2 top-1/2 -translate-y-1/2 w-10 h-10 bg-secondary-fixed text-primary rounded-full flex items-center justify-center hover:scale-110 active:scale-95 transition-transform disabled:opacity-30 disabled:hover:scale-100">
                <span className="material-symbols-outlined text-[22px] icon-fill">arrow_upward</span>
              </button>
            </div>
            <p className="text-[10px] text-center text-outline dark:text-slate-600 mt-2">
              {token ? '컴정이는 컴퓨터정보과 자료 기반으로 답합니다' : '대화는 이 기기에 7일간 보관됩니다'}
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}
