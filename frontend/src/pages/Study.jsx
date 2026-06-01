import { useState, useEffect } from 'react'
import Layout from '../components/layout/Layout'
import { searchStudyGroups, getMyStudyGroups, createStudyGroup, joinStudyGroup, leaveStudyGroup } from '../api/study'

const STATUS_STYLE = {
  OPEN:   'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300',
  FULL:   'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300',
  CLOSED: 'bg-slate-100 dark:bg-slate-800 text-slate-500',
}
const STATUS_LABEL = { OPEN: '모집중', FULL: '마감', CLOSED: '종료' }

export default function Study() {
  const [tab, setTab]         = useState('all')
  const [groups, setGroups]   = useState([])
  const [myGroups, setMyGroups] = useState([])
  const [search, setSearch]   = useState('')
  const [loading, setLoading] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm]       = useState({ name: '', subject: '', description: '', maxMembers: 4 })

  const load = async () => {
    setLoading(true)
    try {
      const [all, my] = await Promise.all([searchStudyGroups(search || undefined), getMyStudyGroups()])
      setGroups(all.data || [])
      setMyGroups(my.data || [])
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    await createStudyGroup(form)
    setShowCreate(false)
    setForm({ name: '', subject: '', description: '', maxMembers: 4 })
    load()
  }

  const handleJoin  = async (id) => { await joinStudyGroup(id);  load() }
  const handleLeave = async (id) => { await leaveStudyGroup(id); load() }

  const list = tab === 'my' ? myGroups : groups

  return (
    <Layout>
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary-fixed">groups</span>
            스터디 매칭
          </h1>
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-secondary-fixed text-primary rounded-lg text-sm font-bold hover:opacity-90">
            <span className="material-symbols-outlined text-[18px]">add</span>스터디 만들기
          </button>
        </div>

        {/* 탭 */}
        <div className="flex gap-2 mb-4">
          {[['all','전체'], ['my','내 스터디']].map(([k, l]) => (
            <button key={k} onClick={() => setTab(k)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors
                ${tab===k ? 'bg-primary dark:bg-primary-container text-white' : 'bg-slate-100 dark:bg-slate-800 text-outline dark:text-slate-400'}`}>
              {l}
            </button>
          ))}
        </div>

        {/* 검색 */}
        {tab === 'all' && (
          <div className="flex gap-2 mb-4">
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="과목명으로 검색…"
              className="flex-1 px-4 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm bg-white dark:bg-slate-900 text-primary dark:text-white" />
            <button onClick={load} className="px-4 py-2 bg-primary text-white rounded-lg text-sm">검색</button>
          </div>
        )}

        {/* 목록 */}
        {loading ? <p className="text-center text-outline dark:text-slate-500 py-12">불러오는 중…</p> :
          list.length === 0 ? <p className="text-center text-outline dark:text-slate-500 py-12">스터디 그룹이 없습니다.</p> :
          <div className="space-y-3">
            {list.map(g => (
              <div key={g.id} className="bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 p-5 shadow-sm">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-bold text-primary dark:text-white">{g.name}</h3>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${STATUS_STYLE[g.status]}`}>{STATUS_LABEL[g.status]}</span>
                      {g.isLeader && <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300">방장</span>}
                    </div>
                    {g.subject && <p className="text-xs text-outline dark:text-slate-400 mb-1">📚 {g.subject}</p>}
                    {g.description && <p className="text-sm text-slate-600 dark:text-slate-300">{g.description}</p>}
                    <p className="text-xs text-outline dark:text-slate-500 mt-2">
                      👤 {g.leaderName} · {g.currentMembers}/{g.maxMembers}명
                    </p>
                  </div>
                  <div>
                    {g.isMember
                      ? !g.isLeader && <button onClick={() => handleLeave(g.id)} className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-outline dark:text-slate-400 hover:border-red-300 hover:text-red-500">나가기</button>
                      : g.status === 'OPEN' && <button onClick={() => handleJoin(g.id)} className="text-xs px-3 py-1.5 rounded-lg bg-primary text-white font-bold hover:opacity-90">참가</button>
                    }
                  </div>
                </div>
              </div>
            ))}
          </div>
        }

        {/* 스터디 만들기 모달 */}
        {showCreate && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 w-full max-w-md">
              <h2 className="font-bold text-primary dark:text-white text-lg mb-4">스터디 만들기</h2>
              <div className="space-y-3">
                <input placeholder="스터디 이름 *" value={form.name}
                  onChange={e => setForm({...form, name: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm bg-white dark:bg-slate-800 text-primary dark:text-white" />
                <input placeholder="과목명" value={form.subject}
                  onChange={e => setForm({...form, subject: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm bg-white dark:bg-slate-800 text-primary dark:text-white" />
                <textarea placeholder="설명" value={form.description} rows={3}
                  onChange={e => setForm({...form, description: e.target.value})}
                  className="w-full px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm bg-white dark:bg-slate-800 text-primary dark:text-white resize-none" />
                <div className="flex items-center gap-3">
                  <label className="text-sm text-outline dark:text-slate-400">정원</label>
                  <input type="number" min={2} max={10} value={form.maxMembers}
                    onChange={e => setForm({...form, maxMembers: +e.target.value})}
                    className="w-20 px-3 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm bg-white dark:bg-slate-800 text-primary dark:text-white" />
                  <span className="text-sm text-outline dark:text-slate-400">명</span>
                </div>
              </div>
              <div className="flex gap-2 mt-5">
                <button onClick={() => setShowCreate(false)} className="flex-1 py-2 border border-slate-200 dark:border-slate-700 rounded-lg text-sm text-outline dark:text-slate-400">취소</button>
                <button onClick={handleCreate} className="flex-1 py-2 bg-secondary-fixed text-primary rounded-lg text-sm font-bold">만들기</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}
