import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getMyClasses, createClass, joinClass } from '../api/classroom'
import { getProgressSummary } from '../api/classProgress'

const ROLE_STYLE = {
  OWNER:   'bg-accent-container text-on-accent-container',
  TEACHER: 'bg-primary-container text-on-primary-container',
  STUDENT: 'bg-surface-container text-on-surface-variant',
}

export default function Classroom() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [classes, setClasses] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [showCreate, setShowCreate] = useState(false)
  const [showJoin, setShowJoin] = useState(false)
  const [form, setForm] = useState({ name: '', subject: '', description: '' })
  const [code, setCode] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const res = await getMyClasses()
      setClasses(res.data || [])
    } finally { setLoading(false) }
  }
  useEffect(() => {
    load()
    getProgressSummary().then(res => setSummary(res.data)).catch(() => {})
  }, [])

  const handleCreate = async () => {
    if (!form.name.trim()) return
    await createClass(form)
    setShowCreate(false)
    setForm({ name: '', subject: '', description: '' })
    load()
  }

  const handleJoin = async () => {
    setError('')
    try {
      await joinClass(code.trim())
      setShowJoin(false)
      setCode('')
      load()
    } catch (e) {
      setError(e?.message || t('classroom.joinError', '참여할 수 없습니다. 코드를 확인하세요.'))
    }
  }

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h1 className="font-space text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-accent">school</span>
            {t('classroom.title', '클래스')}
          </h1>
          <div className="flex gap-2">
            <button onClick={() => setShowJoin(true)} className="btn-secondary">
              <span className="material-symbols-outlined text-[18px]">login</span>
              {t('classroom.join', '참여')}
            </button>
            <button onClick={() => setShowCreate(true)} className="btn-hero">
              <span className="material-symbols-outlined text-[18px]">add</span>
              {t('classroom.create', '개설')}
            </button>
          </div>
        </div>

        {summary && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
            <SummaryStat icon="school" label={t('classroom.statJoined', '참여 중')} value={summary.classesJoined} />
            <SummaryStat icon="cast_for_education" label={t('classroom.statTeaching', '강의 중')} value={summary.classesTeaching} />
            <SummaryStat icon="robot_2" label={t('classroom.statKmate', 'K.MATE 질문')} value={summary.kmateQuestions} />
            <SummaryStat icon="menu_book" label={t('classroom.statMaterials', '자료 수')} value={summary.materials} />
          </div>
        )}

        {loading ? (
          <p className="text-center text-text-muted py-12">{t('common.loading', '불러오는 중…')}</p>
        ) : classes.length === 0 ? (
          <div className="card p-10 text-center text-text-muted">
            {t('classroom.empty', '아직 참여한 클래스가 없습니다. 개설하거나 코드로 참여하세요.')}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {classes.map(c => (
              <div key={c.id} onClick={() => navigate(`/classroom/${c.id}`)}
                   className="card p-5 cursor-pointer hover:scale-[1.01] transition-transform">
                <div className="flex items-start justify-between mb-2">
                  <h3 className="font-bold text-lg text-on-surface dark:text-white">{c.name}</h3>
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${ROLE_STYLE[c.myRole] || ROLE_STYLE.STUDENT}`}>
                    {c.myRole}
                  </span>
                </div>
                {c.subject && <p className="text-sm text-text-muted mb-2">{c.subject}</p>}
                {c.description && <p className="text-sm text-on-surface-variant mb-3 line-clamp-2">{c.description}</p>}
                <div className="flex items-center gap-3 text-xs text-text-muted">
                  <span className="chip">{t('classroom.code', '코드')} {c.code}</span>
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">group</span>{c.memberCount}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

        {showCreate && (
          <Modal title={t('classroom.create', '클래스 개설')} onClose={() => setShowCreate(false)}
                 onSubmit={handleCreate} submitLabel={t('classroom.create', '개설')}>
            <input className="input mb-3" placeholder={t('classroom.name', '클래스 이름')}
                   value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
            <input className="input mb-3" placeholder={t('classroom.subject', '과목')}
                   value={form.subject} onChange={e => setForm({ ...form, subject: e.target.value })} />
            <textarea className="input resize-none" rows={3} placeholder={t('classroom.description', '설명')}
                      value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          </Modal>
        )}

        {showJoin && (
          <Modal title={t('classroom.joinTitle', '코드로 참여')} onClose={() => setShowJoin(false)}
                 onSubmit={handleJoin} submitLabel={t('classroom.join', '참여')}>
            <input className="input" placeholder={t('classroom.codePlaceholder', '6자리 참여 코드')}
                   value={code} onChange={e => setCode(e.target.value.toUpperCase())} maxLength={6} />
            {error && <p className="text-danger text-sm mt-2">{error}</p>}
          </Modal>
        )}
      </div>
    </Layout>
  )
}

function SummaryStat({ icon, label, value }) {
  return (
    <div className="card p-4 flex items-center gap-3">
      <span className="material-symbols-outlined text-accent text-2xl">{icon}</span>
      <div>
        <p className="text-title-lg font-bold text-primary dark:text-white leading-none">{value}</p>
        <p className="text-xs text-text-muted mt-1">{label}</p>
      </div>
    </div>
  )
}

function Modal({ title, children, onClose, onSubmit, submitLabel }) {
  const { t } = useTranslation()
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="card p-6 w-full max-w-md">
        <h2 className="font-bold text-primary dark:text-white text-lg mb-4">{title}</h2>
        {children}
        <div className="flex gap-2 mt-5">
          <button onClick={onClose} className="btn-secondary flex-1 justify-center">{t('common.cancel', '취소')}</button>
          <button onClick={onSubmit} className="btn-hero flex-1 justify-center">{submitLabel}</button>
        </div>
      </div>
    </div>
  )
}
