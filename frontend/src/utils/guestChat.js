// 비로그인 채팅 세션 localStorage 저장 + 7일 자동 만료

const KEY = 'campusflow-guest-chat'
const TTL_MS = 7 * 24 * 60 * 60 * 1000  // 7일

const load = () => {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return { sessions: [] }
    const parsed = JSON.parse(raw)
    return { sessions: Array.isArray(parsed?.sessions) ? parsed.sessions : [] }
  } catch {
    return { sessions: [] }
  }
}

const save = (data) => {
  try { localStorage.setItem(KEY, JSON.stringify(data)) } catch {}
}

/** 만료된 세션 제거 후 목록 반환 (페이지 로드 시 호출) */
export const pruneAndList = () => {
  const now = Date.now()
  const data = load()
  const fresh = data.sessions.filter(s => (now - (s.updatedAt ?? 0)) < TTL_MS)
  if (fresh.length !== data.sessions.length) save({ sessions: fresh })
  return fresh.sort((a, b) => (b.updatedAt ?? 0) - (a.updatedAt ?? 0))
}

const uid = () =>
  (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`)

export const createGuestSession = (firstMessage) => {
  const data = load()
  const id = `gs-${uid()}`
  const sessionKey = `guest_${uid()}`
  const title = makeTitle(firstMessage)
  const session = { id, sessionKey, title, updatedAt: Date.now(), messages: [] }
  data.sessions = [session, ...data.sessions]
  save(data)
  return session
}

export const getGuestSession = (id) => load().sessions.find(s => s.id === id) ?? null

export const appendGuestMessages = (id, userContent, assistantContent) => {
  const data = load()
  const idx = data.sessions.findIndex(s => s.id === id)
  if (idx < 0) return null
  const now = Date.now()
  const s = data.sessions[idx]
  s.messages = [
    ...s.messages,
    { id: `m-${now}-u`, role: 'user', content: userContent, createdAt: now },
    { id: `m-${now}-a`, role: 'assistant', content: assistantContent, createdAt: now + 1 },
  ]
  s.updatedAt = now
  save(data)
  return s
}

export const renameGuestSession = (id, title) => {
  const data = load()
  const s = data.sessions.find(x => x.id === id)
  if (!s) return null
  const t = (title ?? '').trim()
  if (!t) return s
  s.title = t.length > 200 ? t.slice(0, 200) : t
  s.updatedAt = Date.now()
  save(data)
  return s
}

export const deleteGuestSession = (id) => {
  const data = load()
  data.sessions = data.sessions.filter(s => s.id !== id)
  save(data)
}

const makeTitle = (msg) => {
  if (!msg) return '새 채팅'
  const t = msg.trim().replace(/\s+/g, ' ')
  return t.length > 30 ? t.slice(0, 30) + '…' : t
}
