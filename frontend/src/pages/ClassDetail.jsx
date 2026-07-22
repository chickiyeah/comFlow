import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getClass, getClassMembers } from '../api/classroom'
import { getMaterials, uploadMaterial } from '../api/material'
import { getAssignments } from '../api/assignment'
import { getPosts, createPost } from '../api/classpost'

const TABS = [
  { key: 'stream',      icon: 'forum',       label: '스트림' },
  { key: 'materials',   icon: 'menu_book',   label: '자료' },
  { key: 'assignments', icon: 'assignment',  label: '과제' },
  { key: 'people',      icon: 'group',       label: '구성원' },
]

export default function ClassDetail() {
  const { id } = useParams()
  const { t } = useTranslation()
  const [tab, setTab] = useState('stream')
  const [info, setInfo] = useState(null)
  const [data, setData] = useState({ posts: [], materials: [], assignments: [], members: [] })
  const [loading, setLoading] = useState(true)

  const isTeacher = info && info.myRole !== 'STUDENT'

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [cls, posts, materials, assignments, members] = await Promise.all([
        getClass(id), getPosts(id), getMaterials(id), getAssignments(id), getClassMembers(id),
      ])
      setInfo(cls.data)
      setData({
        posts: posts.data || [],
        materials: materials.data || [],
        assignments: assignments.data || [],
        members: members.data || [],
      })
    } finally { setLoading(false) }
  }, [id])

  useEffect(() => { load() }, [load])

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* 헤더 */}
        <div className="card p-6 mb-5 bg-gradient-to-br from-primary to-primary-light text-white border-0">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="font-space text-2xl font-bold">{info?.name || '…'}</h1>
              {info?.subject && <p className="text-white/80 mt-1">{info.subject}</p>}
            </div>
            <span className="chip bg-white/15 text-white border-white/20">
              {t('classroom.code', '코드')} {info?.code}
            </span>
          </div>
        </div>

        {/* 탭 */}
        <div className="flex gap-1 mb-5 border-b border-outline-variant dark:border-[#33355c]">
          {TABS.map(tb => (
            <button key={tb.key} onClick={() => setTab(tb.key)}
              className={`flex items-center gap-1.5 px-4 py-2.5 text-sm font-semibold border-b-2 -mb-px transition-colors
                ${tab === tb.key
                  ? 'border-accent text-primary dark:text-white'
                  : 'border-transparent text-text-muted hover:text-on-surface'}`}>
              <span className="material-symbols-outlined text-[18px]">{tb.icon}</span>
              {t(`classDetail.${tb.key}`, tb.label)}
            </button>
          ))}
        </div>

        {loading ? (
          <p className="text-center text-text-muted py-12">{t('common.loading', '불러오는 중…')}</p>
        ) : (
          <>
            {tab === 'stream' && <StreamTab classId={id} posts={data.posts} onChange={load} />}
            {tab === 'materials' && <MaterialsTab classId={id} materials={data.materials} isTeacher={isTeacher} onChange={load} />}
            {tab === 'assignments' && <AssignmentsTab assignments={data.assignments} />}
            {tab === 'people' && <PeopleTab members={data.members} />}
          </>
        )}
      </div>
    </Layout>
  )
}

function StreamTab({ classId, posts, onChange }) {
  const { t } = useTranslation()
  const [body, setBody] = useState('')
  const post = async () => {
    if (!body.trim()) return
    await createPost(classId, { body })
    setBody('')
    onChange()
  }
  return (
    <div className="space-y-4">
      <div className="card p-4">
        <textarea className="input resize-none" rows={2} value={body} onChange={e => setBody(e.target.value)}
                  placeholder={t('classDetail.postPlaceholder', '학급에 공유하기…')} />
        <div className="flex justify-end mt-2">
          <button onClick={post} className="btn-hero">{t('classDetail.post', '게시')}</button>
        </div>
      </div>
      {posts.length === 0 ? (
        <p className="text-center text-text-muted py-8">{t('classDetail.noPosts', '게시글이 없습니다.')}</p>
      ) : posts.map(p => (
        <div key={p.id} className="card p-4">
          <div className="flex items-center gap-2 mb-1">
            <span className="w-8 h-8 rounded-full bg-accent-container text-on-accent-container flex items-center justify-center text-sm font-bold">
              {p.authorName?.[0] || '?'}
            </span>
            <div>
              <p className="text-sm font-semibold text-on-surface dark:text-white">{p.authorName}</p>
              <p className="text-xs text-text-muted">{new Date(p.createdAt).toLocaleString()}</p>
            </div>
          </div>
          <p className="text-body-md text-on-surface dark:text-[#e6e6f5] whitespace-pre-wrap mt-2">{p.body}</p>
          {p.comments?.length > 0 && (
            <div className="mt-3 pl-3 border-l-2 border-outline-variant dark:border-[#33355c] space-y-1">
              {p.comments.map(c => (
                <p key={c.id} className="text-sm"><span className="font-semibold">{c.authorName}</span> {c.body}</p>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

function MaterialsTab({ classId, materials, isTeacher, onChange }) {
  const { t } = useTranslation()
  const [title, setTitle] = useState('')
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const upload = async () => {
    if (!title.trim()) return
    setBusy(true)
    try {
      await uploadMaterial(classId, { title, file })
      setTitle(''); setFile(null); onChange()
    } finally { setBusy(false) }
  }
  return (
    <div className="space-y-3">
      {isTeacher && (
        <div className="card p-4 flex flex-col sm:flex-row gap-2">
          <input className="input flex-1" value={title} onChange={e => setTitle(e.target.value)}
                 placeholder={t('classDetail.materialTitle', '자료 제목')} />
          <input type="file" onChange={e => setFile(e.target.files?.[0] || null)}
                 className="text-sm text-text-muted file:mr-2 file:px-3 file:py-1.5 file:rounded-full file:border-0 file:bg-primary file:text-white" />
          <button onClick={upload} disabled={busy} className="btn-hero disabled:opacity-50">{t('classDetail.upload', '올리기')}</button>
        </div>
      )}
      {materials.length === 0 ? (
        <p className="text-center text-text-muted py-8">{t('classDetail.noMaterials', '자료가 없습니다.')}</p>
      ) : materials.map(m => (
        <div key={m.id} className="card p-4 flex items-center gap-3">
          <span className="material-symbols-outlined text-accent">{m.hasFile ? 'description' : 'article'}</span>
          <div className="flex-1">
            <p className="font-semibold text-on-surface dark:text-white">{m.title}</p>
            {m.filename && <p className="text-xs text-text-muted">{m.filename}</p>}
          </div>
          {m.hasSummary && <span className="chip-active">AI</span>}
        </div>
      ))}
    </div>
  )
}

function AssignmentsTab({ assignments }) {
  const { t } = useTranslation()
  const STATUS = {
    TURNED_IN: 'bg-success-bg text-success-text', LATE: 'bg-warning-bg text-warning-text',
    GRADED: 'bg-primary-container text-on-primary-container', RETURNED: 'bg-danger-bg text-danger-text',
  }
  if (assignments.length === 0)
    return <p className="text-center text-text-muted py-8">{t('classDetail.noAssignments', '과제가 없습니다.')}</p>
  return (
    <div className="space-y-3">
      {assignments.map(a => (
        <div key={a.id} className="card p-4">
          <div className="flex items-center justify-between">
            <p className="font-semibold text-on-surface dark:text-white">{a.title}</p>
            {a.mySubmissionStatus
              ? <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${STATUS[a.mySubmissionStatus] || ''}`}>{a.mySubmissionStatus}</span>
              : a.draft && <span className="chip">draft</span>}
          </div>
          <div className="flex items-center gap-3 text-xs text-text-muted mt-2">
            <span>{a.points}점</span>
            {a.dueDate && <span>~ {new Date(a.dueDate).toLocaleDateString()}</span>}
          </div>
        </div>
      ))}
    </div>
  )
}

function PeopleTab({ members }) {
  const teachers = members.filter(m => m.role !== 'STUDENT')
  const students = members.filter(m => m.role === 'STUDENT')
  const Row = ({ m }) => (
    <div className="flex items-center gap-3 py-2.5 px-1 border-b border-outline-variant/50 dark:border-[#33355c]/50">
      <span className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-bold">
        {m.name?.[0] || '?'}
      </span>
      <span className="flex-1 text-on-surface dark:text-white">{m.name}</span>
      <span className="text-xs text-text-muted">{m.role}</span>
    </div>
  )
  return (
    <div className="card p-4">
      <p className="text-label-md uppercase text-text-muted mb-1">교사</p>
      {teachers.map(m => <Row key={m.userId} m={m} />)}
      <p className="text-label-md uppercase text-text-muted mt-4 mb-1">학생 ({students.length})</p>
      {students.map(m => <Row key={m.userId} m={m} />)}
    </div>
  )
}
