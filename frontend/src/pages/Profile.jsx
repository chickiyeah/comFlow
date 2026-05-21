import { useState, useEffect } from 'react'
import Layout from '../components/layout/Layout'
import { getProfile, syncProfile, disableSync } from '../api/profile'

export default function Profile() {
  const [profile, setProfile]   = useState(null)
  const [loading, setLoading]   = useState(true)
  const [syncing, setSyncing]   = useState(false)
  const [showPwModal, setShowPwModal] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState('')

  useEffect(() => {
    getProfile()
      .then(r => setProfile(r.data.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleSyncToggle = () => {
    if (profile?.intranetSyncEnabled) {
      // 연동 해제
      disableSync()
        .then(r => { setProfile(r.data.data); setSuccess('학교 포털 연동이 해제되었습니다.') })
        .catch(() => setError('연동 해제 실패'))
    } else {
      // 연동 활성화 → 비밀번호 모달
      setError('')
      setPassword('')
      setShowPwModal(true)
    }
  }

  const handleSync = async () => {
    if (!password) { setError('학교 포털 비밀번호를 입력해주세요.'); return }
    setSyncing(true)
    setError('')
    try {
      const r = await syncProfile(password)
      setProfile(r.data.data)
      setShowPwModal(false)
      setPassword('')
      setSuccess('학교 포털 정보가 동기화되었습니다!')
    } catch (e) {
      const msg = e.response?.data?.message || '동기화 실패. 비밀번호를 확인하세요.'
      setError(msg)
    } finally {
      setSyncing(false)
    }
  }

  const handleManualSync = () => {
    setError('')
    setPassword('')
    setShowPwModal(true)
  }

  if (loading) {
    return <Layout><div className="flex items-center justify-center min-h-[60vh]">
      <div className="w-8 h-8 border-4 border-primary border-t-secondary-fixed rounded-full animate-spin" />
    </div></Layout>
  }

  const syncEnabled = profile?.intranetSyncEnabled

  return (
    <Layout>
      <div className="max-w-lg mx-auto">
        <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white mb-6 flex items-center gap-2">
          <span className="material-symbols-outlined text-secondary-fixed">manage_accounts</span>
          프로필 관리
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
                  alt="학생증 사진"
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
              <p className="text-sm text-outline dark:text-slate-400">{profile?.department} · {profile?.grade}학년 {profile?.semester}학기</p>
              <p className="text-xs text-outline dark:text-slate-500 mt-0.5 font-mono">{profile?.studentId}</p>
            </div>
          </div>

          {/* 연락처 */}
          <div className="border-t border-slate-50 dark:border-slate-800 px-6 py-4 space-y-2">
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">phone</span>
              <span className="text-sm text-primary dark:text-slate-200">{profile?.phone || '등록되지 않음'}</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-[18px] text-outline dark:text-slate-400">mail</span>
              <span className="text-sm text-primary dark:text-slate-200">{profile?.email || '등록되지 않음'}</span>
            </div>
          </div>
        </div>

        {/* 학교 포털 연동 카드 */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-100 dark:border-slate-800 shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${syncEnabled ? 'bg-green-100 dark:bg-green-900/30' : 'bg-slate-100 dark:bg-slate-800'}`}>
                <span className={`material-symbols-outlined text-[20px] ${syncEnabled ? 'text-green-600 dark:text-green-400' : 'text-outline dark:text-slate-400'}`}>
                  sync
                </span>
              </div>
              <div>
                <p className="font-bold text-primary dark:text-white text-sm">학교 포털 연동</p>
                <p className="text-xs text-outline dark:text-slate-400">
                  {syncEnabled ? `연동됨 · ${profile?.intranetSyncedAt ? new Date(profile.intranetSyncedAt).toLocaleDateString('ko') : ''}` : '미연동'}
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
                <span>이름·전화번호·이메일·학생증 사진이 학교 포털과 연동됩니다. 비밀번호는 서버에 저장되지 않습니다.</span>
              </div>
              <button
                onClick={handleManualSync}
                className="w-full py-2.5 flex items-center justify-center gap-2 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium text-primary dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
              >
                <span className="material-symbols-outlined text-[18px]">refresh</span>
                지금 다시 동기화
              </button>
            </div>
          ) : (
            <div className="text-xs text-outline dark:text-slate-400 bg-slate-50 dark:bg-slate-800 rounded-xl px-4 py-3">
              연동하면 학교 포털에서 이름·연락처·학생증 사진을 자동으로 가져옵니다.<br />
              학번은 현재 계정의 학번이 사용되며, 학교 포털 비밀번호만 입력하면 됩니다.
            </div>
          )}
        </div>
      </div>

      {/* 비밀번호 입력 모달 */}
      {showPwModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="font-bold text-primary dark:text-white text-lg mb-1">학교 포털 연동</h3>
            <p className="text-sm text-outline dark:text-slate-400 mb-5">
              학번: <span className="font-mono font-bold text-primary dark:text-white">{profile?.studentId}</span><br />
              학교 포털 비밀번호를 입력하세요.
            </p>

            {error && (
              <div className="mb-3 px-3 py-2 bg-red-50 dark:bg-red-900/20 rounded-lg text-red-600 dark:text-red-400 text-sm">
                {error}
              </div>
            )}

            <input
              type="password"
              placeholder="학교 포털 비밀번호"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSync()}
              className="w-full px-4 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm bg-white dark:bg-slate-800 text-primary dark:text-white mb-4 focus:outline-none focus:border-primary dark:focus:border-secondary-fixed"
              autoFocus
            />

            <div className="text-xs text-outline dark:text-slate-500 mb-4 flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[14px]">lock</span>
              비밀번호는 학교 포털 인증에만 사용되며 저장되지 않습니다.
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => { setShowPwModal(false); setError('') }}
                className="flex-1 py-2.5 border border-slate-200 dark:border-slate-700 rounded-xl text-sm text-outline dark:text-slate-400"
              >
                취소
              </button>
              <button
                onClick={handleSync}
                disabled={syncing}
                className="flex-1 py-2.5 bg-secondary-fixed text-primary rounded-xl text-sm font-bold disabled:opacity-60 flex items-center justify-center gap-1.5"
              >
                {syncing ? (
                  <><span className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />동기화 중</>
                ) : (
                  <><span className="material-symbols-outlined text-[16px]">sync</span>연동하기</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
