import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { getProfile, syncProfile, disableSync, updateAcademic } from '../api/profile'
import { getSessions, revokeSession, revokeOtherSessions } from '../api/session'
import { pushSupported, isPushEnabled, enablePush, disablePush } from '../lib/push'
import { getNotifPrefs, updateNotifPrefs } from '../api/notification'
import { getMyGamification } from '../api/gamification'

export default function Profile() {
  const { t } = useTranslation()
  const [profile, setProfile]   = useState(null)
  const [loading, setLoading]   = useState(true)
  const [syncing, setSyncing]   = useState(false)
  const [showPwModal, setShowPwModal] = useState(false)
  const [showAcademicModal, setShowAcademicModal] = useState(false)
  const [password, setPassword] = useState('')
  const [studentIdInput, setStudentIdInput] = useState('')
  const [gradeInput, setGradeInput]     = useState(1)
  const [semesterInput, setSemesterInput] = useState(1)
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState('')
  const [sessions, setSessions] = useState([])
  const [sessionsLoading, setSessionsLoading] = useState(true)
  const [pushOn, setPushOn] = useState(false)
  const [pushBusy, setPushBusy] = useState(false)
  const [notifPrefs, setNotifPrefs] = useState({ jobAlert: true, notice: true })
  const [gami, setGami] = useState(null)

  useEffect(() => {
    getProfile()
      .then(r => setProfile(r.data))
      .catch(() => {})
      .finally(() => setLoading(false))
    loadSessions()
    isPushEnabled().then(setPushOn)
    getNotifPrefs().then(r => r.data && setNotifPrefs(r.data)).catch(() => {})
    getMyGamification().then(r => setGami(r.data)).catch(() => {})
  }, [])

  const toggleNotifPref = async (key) => {
    const next = { ...notifPrefs, [key]: !notifPrefs[key] }
    setNotifPrefs(next)
    try { await updateNotifPrefs(next) } catch { setNotifPrefs(notifPrefs) }
  }

  const togglePush = async () => {
    if (pushBusy) return
    setPushBusy(true); setError(''); setSuccess('')
    try {
      if (pushOn) { await disablePush(); setPushOn(false) }
      else { await enablePush(); setPushOn(true); setSuccess(t('profile.push.enabled')) }
    } catch (e) {
      if (e.message === 'denied') setError(t('profile.push.denied'))
      else if (e.message === 'no-key') setError(t('profile.push.noKey'))
      else setError(t('profile.push.failed'))
    } finally { setPushBusy(false) }
  }

  const loadSessions = () => {
    setSessionsLoading(true)
    getSessions()
      .then(r => setSessions(r.data ?? []))
      .catch(() => setSessions([]))
      .finally(() => setSessionsLoading(false))
  }

  const handleRevokeSession = async (id) => {
    if (!confirm(t('profile.sessions.confirmRevoke'))) return
    try { await revokeSession(id); loadSessions() } catch { /* ignore */ }
  }

  const handleRevokeOthers = async () => {
    if (!confirm(t('profile.sessions.confirmRevokeOthers'))) return
    try { await revokeOtherSessions(); loadSessions() } catch { /* ignore */ }
  }

  const sessionTime = (iso) => {
    if (!iso) return ''
    try { return new Date(iso).toLocaleString('ko', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }
    catch { return '' }
  }

  const handleSyncToggle = () => {
    if (profile?.intranetSyncEnabled) {
      disableSync()
        .then(r => { setProfile(r.data); setSuccess(t('profile.syncDisabled')) })
        .catch(() => setError(t('profile.syncDisableFailed')))
    } else {
      setError(''); setPassword(''); setStudentIdInput(profile?.studentId ?? '')
      setShowPwModal(true)
    }
  }

  const handleSync = async () => {
    if (!password) { setError(t('profile.passwordRequired')); return }
    setSyncing(true)
    setError('')
    try {
      const syncRes = await syncProfile(password, studentIdInput.trim())
      setProfile(syncRes.data)
      setShowPwModal(false)
      setPassword('')
      setStudentIdInput('')
      setSuccess(t('profile.syncSuccess'))
    } catch (e) {
      const msg = e.response?.data?.message || t('profile.syncFailed')
      setError(msg)
    } finally {
      setSyncing(false)
    }
  }

  const handleManualSync = () => {
    setError(''); setPassword(''); setStudentIdInput(profile?.studentId ?? '')
    setShowPwModal(true)
  }

  const handleAcademicSave = async () => {
    try {
      const r = await updateAcademic(gradeInput, semesterInput)
      setProfile(r.data)
      setShowAcademicModal(false)
      setSuccess(t('profile.academicUpdated'))
    } catch {
      setError(t('profile.academicUpdateFailed'))
    }
  }

  if (loading) {
    return <Layout><div className="flex items-center justify-center min-h-[60vh]">
      <div className="w-8 h-8 border-4 border-primary border-t-secondary-fixed rounded-full animate-spin" />
    </div></Layout>
  }

  const syncEnabled = profile?.intranetSyncEnabled
  const SHOW_PORTAL_SYNC = false  // 발표용 임시: 학교 포털 연동 UI 숨김 (복구 시 true)

  return (
    <Layout>
      <div className="max-w-lg mx-auto">
        <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white mb-6 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary-fixed">manage_accounts</span>
          {t('profile.title')}
        </h1>

        {/* 성공/에러 메시지 */}
        {success && (
          <div className="mb-4 px-4 py-3 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl text-green-700 dark:text-green-300 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">check_circle</span>{success}
          </div>
        )}
        {error && !showPwModal && (
          <div className="mb-4 px-4 py-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl text-red-700 dark:text-red-300 text-sm flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">error</span>{error}
          </div>
        )}

        {/* 프로필 카드 */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden mb-4">
          {/* 상단 — 사진 + 기본 정보 */}
          <div className="p-6 flex items-center gap-5">
            <div className="relative shrink-0">
              {profile?.profileImageData ? (
                <img
                  src={`data:image/png;base64,${profile.profileImageData}`}
                  alt={t('profile.studentPhotoAlt')}
                  className="w-20 h-20 rounded-2xl object-cover border-2 border-secondary-fixed shadow-md"
                />
              ) : (
                <div className="w-20 h-20 rounded-2xl bg-primary-container dark:bg-slate-800 flex items-center justify-center border-2 border-slate-200 dark:border-slate-700">
                  <span className="material-symbols-outlined text-4xl text-primary dark:text-secondary-fixed icon-fill">person</span>
                </div>
              )}
              {syncEnabled && (
                <span className="absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white dark:border-slate-900 flex items-center justify-center">
                  <span className="material-symbols-outlined text-white text-[12px]">check</span>
                </span>
              )}
            </div>
            <div>
              <h2 className="text-xl font-bold text-primary dark:text-white">{profile?.name ?? '—'}</h2>
              <div className="flex items-center gap-2">
                <p className="text-sm text-outline dark:text-slate-400">{profile?.department} · {t('profile.gradeSemester', { grade: profile?.grade, semester: profile?.semester })}</p>
                <button
                  onClick={() => { setGradeInput(profile?.grade ?? 1); setSemesterInput(profile?.semester ?? 1); setShowAcademicModal(true) }}
                  className="text-outline dark:text-slate-500 hover:text-primary dark:hover:text-white transition-colors"
                  title={t('profile.editAcademic')}
                >
                  <span className="material-symbols-outlined text-[14px]">edit</span>
                </button>
              </div>
              <p className="text-xs text-outline dark:text-slate-500 mt-0.5 font-mono">{profile?.studentId}</p>
            </div>
          </div>

          {/* 연락처 */}
          <div className="border-t border-slate-50 dark:border-slate-800 px-6 py-4 space-y-2">
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">phone</span>
              <span className="text-sm text-primary dark:text-slate-200">{profile?.phone || t('profile.notRegistered')}</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">mail</span>
              <span className="text-sm text-primary dark:text-slate-200">{profile?.email || t('profile.notRegistered')}</span>
            </div>
          </div>
        </div>

        {/* 학교 포털 연동 카드 — 발표용으로 임시 숨김 (SHOW_PORTAL_SYNC) */}
        {SHOW_PORTAL_SYNC && (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${syncEnabled ? 'bg-green-100 dark:bg-green-900/30' : 'bg-slate-100 dark:bg-slate-800'}`}>
                <span className={`material-symbols-outlined text-[20px] ${syncEnabled ? 'text-green-600 dark:text-green-400' : 'text-outline dark:text-slate-400'}`}>
                  sync
                </span>
              </div>
              <div>
                <p className="font-bold text-primary dark:text-white text-sm">{t('profile.portalSync')}</p>
                <p className="text-xs text-outline dark:text-slate-400">
                  {syncEnabled ? t('profile.syncedAt', { date: profile?.intranetSyncedAt ? new Date(profile.intranetSyncedAt).toLocaleDateString('ko') : '' }) : t('profile.notSynced')}
                </p>
              </div>
            </div>

            {/* 토글 스위치 */}
            <button
              onClick={handleSyncToggle}
              className={`relative inline-flex h-7 w-13 items-center rounded-full transition-colors duration-200 focus:outline-none
                ${syncEnabled ? 'bg-green-500' : 'bg-slate-300 dark:bg-slate-600'}`}
              style={{ minWidth: 52 }}
            >
              <span className={`inline-block h-5 w-5 rounded-full bg-white shadow-sm transform transition-transform duration-200
                ${syncEnabled ? 'translate-x-7' : 'translate-x-1'}`} />
            </button>
          </div>

          {syncEnabled ? (
            <div className="space-y-3">
              <div className="bg-green-50 dark:bg-green-900/20 rounded-xl px-4 py-3 text-xs text-green-700 dark:text-green-300 flex items-start gap-2">
                <span className="material-symbols-outlined text-[16px] mt-0.5">info</span>
                <span>{t('profile.syncInfo')}</span>
              </div>
              <button
                onClick={handleManualSync}
                className="w-full py-2.5 flex items-center justify-center gap-2 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-primary dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
              >
                <span className="material-symbols-outlined text-[18px]">refresh</span>
                {t('profile.resyncNow')}
              </button>
            </div>
          ) : (
            <div className="text-xs text-outline dark:text-slate-400 bg-slate-50 dark:bg-slate-800 rounded-xl px-4 py-3">
              {t('profile.notSyncedInfoLine1')}<br />
              {t('profile.notSyncedInfoLine2')}
            </div>
          )}
        </div>
        )}

        {/* 내 성취 (게이미피케이션) */}
        {gami && (
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6 mt-5">
            <div className="flex items-center justify-between mb-3">
              <p className="font-bold text-primary dark:text-white text-sm flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary-fixed">trophy</span>{t('profile.gami.title')}
              </p>
              <span className="text-xs font-black px-2.5 py-1 rounded-full bg-primary dark:bg-primary-container text-white">Lv.{gami.level}</span>
            </div>
            <div className="flex items-center justify-between text-xs text-outline dark:text-slate-400 mb-1">
              <span>{t('profile.gami.points', { points: gami.points })}</span>
              <span>{t('profile.gami.toNext', { count: gami.toNextLevel })}</span>
            </div>
            <div className="h-2 rounded-full bg-surface-container dark:bg-slate-800 overflow-hidden mb-4">
              <div className="h-full rounded-full bg-secondary-fixed" style={{ width: `${gami.levelProgress}%` }} />
            </div>
            <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
              {gami.badges.map(b => (
                <div key={b.code} className={`flex flex-col items-center gap-1 p-2 rounded-xl ${b.earned ? 'bg-secondary-container/30 dark:bg-secondary-fixed/10' : 'bg-surface-container-low dark:bg-slate-800 opacity-40'}`} title={t('profile.gami.badge.' + b.code)}>
                  <span className={`material-symbols-outlined ${b.earned ? 'text-secondary dark:text-secondary-fixed' : 'text-outline dark:text-slate-600'}`}>{b.icon}</span>
                  <span className="text-[9px] text-center leading-tight text-on-surface-variant dark:text-slate-400">{t('profile.gami.badge.' + b.code)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 푸시 알림 */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6 mt-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${pushOn ? 'bg-green-100 dark:bg-green-900/30' : 'bg-slate-100 dark:bg-slate-800'}`}>
                <span className={`material-symbols-outlined text-[20px] ${pushOn ? 'text-green-600 dark:text-green-400' : 'text-outline dark:text-slate-400'}`}>notifications_active</span>
              </div>
              <div>
                <p className="font-bold text-primary dark:text-white text-sm">{t('profile.push.title')}</p>
                <p className="text-xs text-outline dark:text-slate-400">
                  {pushSupported() ? t('profile.push.subtitle') : t('profile.push.unsupported')}
                </p>
              </div>
            </div>
            {pushSupported() && (
              <button
                onClick={togglePush}
                disabled={pushBusy}
                className={`relative inline-flex h-7 w-13 items-center rounded-full transition-colors duration-200 focus:outline-none disabled:opacity-50 ${pushOn ? 'bg-green-500' : 'bg-slate-300 dark:bg-slate-600'}`}
                style={{ minWidth: 52 }}
              >
                <span className={`inline-block h-5 w-5 rounded-full bg-white shadow-sm transform transition-transform duration-200 ${pushOn ? 'translate-x-7' : 'translate-x-1'}`} />
              </button>
            )}
          </div>
        </div>

        {/* 알림 수신 설정 */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6 mt-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
              <span className="material-symbols-outlined text-[20px] text-outline dark:text-slate-300">tune</span>
            </div>
            <div>
              <p className="font-bold text-primary dark:text-white text-sm">{t('profile.notifPref.title')}</p>
              <p className="text-xs text-outline dark:text-slate-400">{t('profile.notifPref.subtitle')}</p>
            </div>
          </div>
          {[
            { key: 'jobAlert', icon: 'work', label: t('profile.notifPref.jobAlert') },
            { key: 'notice', icon: 'campaign', label: t('profile.notifPref.notice') },
          ].map(row => (
            <div key={row.key} className="flex items-center justify-between py-2.5 border-t border-slate-50 dark:border-slate-800">
              <div className="flex items-center gap-2.5">
                <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">{row.icon}</span>
                <span className="text-sm text-on-surface dark:text-slate-200">{row.label}</span>
              </div>
              <button onClick={() => toggleNotifPref(row.key)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${notifPrefs[row.key] ? 'bg-green-500' : 'bg-slate-300 dark:bg-slate-600'}`}>
                <span className={`inline-block h-4 w-4 rounded-full bg-white shadow-sm transform transition-transform ${notifPrefs[row.key] ? 'translate-x-6' : 'translate-x-1'}`} />
              </button>
            </div>
          ))}
          <p className="text-[11px] text-outline dark:text-slate-500 mt-2">{t('profile.notifPref.alwaysOn')}</p>
        </div>

        {/* 접속 기기 관리 */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6 mt-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                <span className="material-symbols-outlined text-[20px] text-outline dark:text-slate-300">devices</span>
              </div>
              <div>
                <p className="font-bold text-primary dark:text-white text-sm">{t('profile.sessions.title')}</p>
                <p className="text-xs text-outline dark:text-slate-400">{t('profile.sessions.subtitle')}</p>
              </div>
            </div>
            {sessions.length > 1 && (
              <button onClick={handleRevokeOthers}
                className="text-xs font-semibold text-error hover:underline shrink-0">
                {t('profile.sessions.logoutOthers')}
              </button>
            )}
          </div>

          {sessionsLoading ? (
            <div className="space-y-2">
              {[1,2].map(i => <div key={i} className="h-14 bg-slate-50 dark:bg-slate-800 rounded-xl animate-pulse" />)}
            </div>
          ) : sessions.length === 0 ? (
            <p className="text-sm text-outline dark:text-slate-400 text-center py-4">{t('profile.sessions.empty')}</p>
          ) : (
            <div className="space-y-2">
              {sessions.map(s => (
                <div key={s.id} className="flex items-center gap-3 px-4 py-3 rounded-xl border border-slate-100 dark:border-slate-800">
                  <span className="material-symbols-outlined text-[20px] text-outline dark:text-slate-400">
                    {/iphone|ipad|android|ios/i.test(s.device) ? 'smartphone' : 'computer'}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-primary dark:text-white truncate">{s.device}</p>
                      {s.current && (
                        <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 shrink-0">
                          {t('profile.sessions.current')}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-outline dark:text-slate-500">
                      {s.ip || '—'} · {t('profile.sessions.lastSeen', { time: sessionTime(s.lastSeenAt) })}
                    </p>
                  </div>
                  {!s.current && (
                    <button onClick={() => handleRevokeSession(s.id)}
                      className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-outline dark:text-slate-400 hover:border-red-300 hover:text-red-500 shrink-0">
                      {t('profile.sessions.logout')}
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 학년/학기 수정 모달 */}
      {showAcademicModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 w-full max-w-xs shadow-xl">
            <h3 className="font-bold text-primary dark:text-white text-lg mb-4">{t('profile.editAcademic')}</h3>
            <div className="flex gap-3 mb-5">
              <div className="flex-1">
                <label className="text-xs text-outline dark:text-slate-400 block mb-1.5">{t('profile.grade')}</label>
                <select value={gradeInput} onChange={e => setGradeInput(Number(e.target.value))}
                  className="w-full px-3 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none focus:border-primary">
                  {[1,2].map(g => <option key={g} value={g}>{t('profile.gradeN', { n: g })}</option>)}
                </select>
              </div>
              <div className="flex-1">
                <label className="text-xs text-outline dark:text-slate-400 block mb-1.5">{t('profile.semester')}</label>
                <select value={semesterInput} onChange={e => setSemesterInput(Number(e.target.value))}
                  className="w-full px-3 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none focus:border-primary">
                  {[1,2].map(s => <option key={s} value={s}>{t('profile.semesterN', { n: s })}</option>)}
                </select>
              </div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => setShowAcademicModal(false)}
                className="flex-1 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-outline dark:text-slate-400">
                {t('common.cancel')}
              </button>
              <button onClick={handleAcademicSave}
                className="flex-1 py-2.5 bg-secondary-fixed text-primary rounded-xl text-sm font-bold">
                {t('common.save')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 비밀번호 입력 모달 — 포털 연동 숨김과 함께 비활성 */}
      {SHOW_PORTAL_SYNC && showPwModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="font-bold text-primary dark:text-white text-lg mb-1">{t('profile.portalSync')}</h3>
            <p className="text-sm text-outline dark:text-slate-400 mb-4">
              {t('profile.modalPrompt')}
            </p>

            {error && (
              <div className="mb-3 px-3 py-2 bg-red-50 dark:bg-red-900/20 rounded-lg text-red-600 dark:text-red-400 text-sm">
                {error}
              </div>
            )}

            <div className="mb-3">
              <label className="text-xs text-outline dark:text-slate-400 block mb-1.5">{t('profile.studentIdLabel')}</label>
              <input
                type="text"
                placeholder={t('profile.studentIdPlaceholder')}
                value={studentIdInput}
                onChange={e => setStudentIdInput(e.target.value)}
                className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none focus:border-primary dark:focus:border-secondary-fixed font-mono"
                autoFocus
              />
            </div>

            <div className="mb-4">
              <label className="text-xs text-outline dark:text-slate-400 block mb-1.5">{t('profile.portalPassword')}</label>
              <input
                type="password"
                placeholder={t('profile.portalPassword')}
                value={password}
                onChange={e => setPassword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSync()}
                className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white focus:outline-none focus:border-primary dark:focus:border-secondary-fixed"
              />
            </div>

            <div className="text-xs text-outline dark:text-slate-500 mb-4 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[14px]">lock</span>
              {t('profile.passwordNotStored')}
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => { setShowPwModal(false); setError('') }}
                className="flex-1 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-outline dark:text-slate-400"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleSync}
                disabled={syncing}
                className="flex-1 py-2.5 bg-secondary-fixed text-primary rounded-xl text-sm font-bold disabled:opacity-60 flex items-center justify-center gap-1.5"
              >
                {syncing ? (
                  <><span className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />{t('profile.syncing')}</>
                ) : (
                  <><span className="material-symbols-outlined text-[16px]">sync</span>{t('profile.connect')}</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
