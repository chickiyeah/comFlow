import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { register } from '../../api/auth'
import useThemeStore from '../../store/themeStore'

const LANGUAGES = [
  { code: 'ko', label: '한국어',     flag: '🇰🇷' },
  { code: 'en', label: 'English',    flag: '🇺🇸' },
  { code: 'zh', label: '中文',       flag: '🇨🇳' },
  { code: 'ja', label: '日本語',     flag: '🇯🇵' },
  { code: 'vi', label: 'Tiếng Việt', flag: '🇻🇳' },
]

const STEPS = ['role', 'info', 'password']

export default function Register() {
  const navigate = useNavigate()
  const { dark, toggle } = useThemeStore()
  const { i18n } = useTranslation()

  const [step, setStep] = useState(0)          // 0 = 구분 선택, 1 = 정보 입력, 2 = 비밀번호
  const [showLang, setShowLang] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const [form, setForm] = useState({
    role: '',            // ROLE_STUDENT | ROLE_PROFESSOR
    username: '',        // 학번 or 교번
    name: '',
    password: '',
    passwordConfirm: '',
  })

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }))

  const currentLang = LANGUAGES.find(l => l.code === i18n.language) ?? LANGUAGES[0]

  /* ── 각 스텝 유효성 ── */
  const canNext = () => {
    if (step === 0) return !!form.role
    if (step === 1) return form.username.trim().length >= 4 && form.name.trim().length >= 2
    return false
  }

  const handleSubmit = async () => {
    if (form.password.length < 6) { setError('비밀번호는 6자 이상이어야 합니다.'); return }
    if (form.password !== form.passwordConfirm) { setError('비밀번호가 일치하지 않습니다.'); return }
    setError('')
    setLoading(true)
    try {
      await register({
        username: form.username.trim(),
        password: form.password,
        name: form.name.trim(),
        role: form.role,
      })
      setDone(true)
    } catch (err) {
      setError(err?.message || '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background dark:bg-[#0b0e14] flex items-center justify-center p-4 transition-colors duration-300">

      {/* Top-right controls */}
      <div className="fixed top-4 right-4 flex items-center gap-2 z-50">
        <div className="relative">
          <button
            onClick={() => setShowLang(v => !v)}
            className="flex items-center gap-1.5 px-3 py-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:scale-105 transition-transform text-sm font-medium"
          >
            <span>{currentLang.flag}</span>
            <span>{currentLang.code.toUpperCase()}</span>
            <span className="material-symbols-outlined text-[14px]">expand_more</span>
          </button>
          {showLang && (
            <div className="absolute right-0 top-11 bg-white dark:bg-slate-900 rounded-2xl shadow-xl border border-slate-100 dark:border-slate-800 py-1.5 w-44 overflow-hidden">
              {LANGUAGES.map(lang => (
                <button key={lang.code} onClick={() => { i18n.changeLanguage(lang.code); setShowLang(false) }}
                  className={`w-full text-left px-4 py-2.5 text-sm flex items-center gap-3 hover:bg-surface-container-low dark:hover:bg-slate-800 transition-colors ${i18n.language === lang.code ? 'text-primary dark:text-secondary-fixed font-bold' : 'text-on-surface dark:text-slate-300'}`}>
                  <span>{lang.flag}</span><span>{lang.label}</span>
                  {i18n.language === lang.code && <span className="material-symbols-outlined text-[16px] ml-auto">check</span>}
                </button>
              ))}
            </div>
          )}
        </div>
        <button onClick={toggle} className="p-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:scale-110 transition-transform">
          <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
        </button>
      </div>

      <div className="w-full max-w-sm">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary dark:bg-primary-container mb-3">
            <span className="text-secondary-fixed font-black text-xl font-['Space_Grotesk']">CF</span>
          </div>
          <h1 className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">회원가입</h1>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">CampusFlow 계정을 만들어보세요.</p>
        </div>

        {/* Step indicator */}
        {!done && (
          <div className="flex items-center justify-center gap-2 mb-8">
            {STEPS.map((_, i) => (
              <div key={i} className={`h-1.5 rounded-full transition-all duration-300 ${i <= step ? 'bg-primary dark:bg-secondary-fixed' : 'bg-outline-variant dark:bg-slate-700'} ${i === step ? 'w-8' : 'w-4'}`} />
            ))}
          </div>
        )}

        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 p-8 shadow-sm">

          {/* ── 완료 ── */}
          {done ? (
            <div className="text-center py-4 space-y-4">
              <span className="material-symbols-outlined text-[64px] text-secondary dark:text-secondary-fixed">check_circle</span>
              <p className="font-['Space_Grotesk'] text-xl font-bold text-primary dark:text-white">가입 완료!</p>
              <p className="text-sm text-on-surface-variant dark:text-slate-400">
                {form.name}님의 계정이 생성되었습니다.<br />이제 로그인하세요.
              </p>
              <button onClick={() => navigate('/login')} className="w-full py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm hover:scale-[1.02] active:scale-95 transition-transform">
                로그인하러 가기
              </button>
            </div>
          ) : (

            <div className="space-y-5">

              {/* ── Step 0: 구분 선택 ── */}
              {step === 0 && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">구분 선택</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-4">학생 또는 교수 / 교직원으로 가입합니다.</p>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'ROLE_STUDENT',   icon: 'school',       label: '학생',     sub: '학번으로 가입' },
                      { value: 'ROLE_PROFESSOR',  icon: 'person_book',  label: '교수 / 교직원', sub: '교번으로 가입' },
                    ].map(r => (
                      <button
                        key={r.value}
                        type="button"
                        onClick={() => set('role', r.value)}
                        className={`p-5 rounded-2xl border-2 flex flex-col items-center gap-2 transition-all active:scale-95 ${
                          form.role === r.value
                            ? 'border-primary dark:border-secondary-fixed bg-primary/5 dark:bg-secondary-fixed/10'
                            : 'border-outline-variant dark:border-slate-700 hover:border-primary/50 dark:hover:border-slate-500'
                        }`}
                      >
                        <span className={`material-symbols-outlined text-3xl ${form.role === r.value ? 'text-primary dark:text-secondary-fixed' : 'text-outline dark:text-slate-400'}`}>{r.icon}</span>
                        <span className={`font-bold text-sm ${form.role === r.value ? 'text-primary dark:text-white' : 'text-on-surface dark:text-slate-300'}`}>{r.label}</span>
                        <span className="text-[10px] text-on-surface-variant dark:text-slate-500">{r.sub}</span>
                        {form.role === r.value && (
                          <span className="material-symbols-outlined text-[16px] text-primary dark:text-secondary-fixed">check_circle</span>
                        )}
                      </button>
                    ))}
                  </div>
                </>
              )}

              {/* ── Step 1: 기본 정보 ── */}
              {step === 1 && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">기본 정보</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">
                      {form.role === 'ROLE_STUDENT' ? '학번과 이름을 입력하세요.' : '교번과 이름을 입력하세요.'}
                    </p>
                  </div>

                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">
                      {form.role === 'ROLE_STUDENT' ? '학번' : '교번'}
                    </label>
                    <input
                      autoFocus
                      type="text"
                      value={form.username}
                      onChange={e => set('username', e.target.value)}
                      placeholder={form.role === 'ROLE_STUDENT' ? '예: 2024010001' : '예: P2024001'}
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
                    />
                    <p className="text-[11px] text-on-surface-variant dark:text-slate-500 mt-1">
                      {form.role === 'ROLE_STUDENT' ? '입학년도 + 학과코드 + 일련번호 형식' : '학교에서 발급된 교번을 입력하세요'}
                    </p>
                  </div>

                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">이름</label>
                    <input
                      type="text"
                      value={form.name}
                      onChange={e => set('name', e.target.value)}
                      placeholder="실명을 입력해주세요"
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
                    />
                  </div>
                </>
              )}

              {/* ── Step 2: 비밀번호 ── */}
              {step === 2 && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">비밀번호 설정</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">6자 이상의 비밀번호를 설정하세요.</p>
                  </div>

                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">비밀번호</label>
                    <input
                      autoFocus
                      type="password"
                      value={form.password}
                      onChange={e => { set('password', e.target.value); setError('') }}
                      placeholder="6자 이상 입력"
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all"
                    />
                  </div>

                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">비밀번호 확인</label>
                    <input
                      type="password"
                      value={form.passwordConfirm}
                      onChange={e => { set('passwordConfirm', e.target.value); setError('') }}
                      placeholder="동일한 비밀번호 재입력"
                      className={`w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border rounded-xl text-sm focus:outline-none focus:ring-2 transition-all dark:text-on-surface ${
                        form.passwordConfirm && form.password !== form.passwordConfirm
                          ? 'border-error focus:ring-error/30'
                          : 'border-outline-variant dark:border-slate-700 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30'
                      }`}
                    />
                    {form.passwordConfirm && form.password !== form.passwordConfirm && (
                      <p className="text-[11px] text-error mt-1">비밀번호가 일치하지 않습니다.</p>
                    )}
                    {form.passwordConfirm && form.password === form.passwordConfirm && (
                      <p className="text-[11px] text-secondary dark:text-secondary-fixed mt-1 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">check_circle</span>비밀번호가 일치합니다.
                      </p>
                    )}
                  </div>

                  {/* 입력 요약 */}
                  <div className="p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl space-y-1.5 border border-outline-variant dark:border-slate-700">
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-2">입력 정보 확인</p>
                    {[
                      { label: '구분',                        val: form.role === 'ROLE_STUDENT' ? '학생' : '교수/교직원' },
                      { label: form.role === 'ROLE_STUDENT' ? '학번' : '교번', val: form.username },
                      { label: '이름',                        val: form.name },
                    ].map(i => (
                      <div key={i.label} className="flex justify-between text-sm">
                        <span className="text-on-surface-variant dark:text-slate-400">{i.label}</span>
                        <span className="font-medium text-primary dark:text-white">{i.val}</span>
                      </div>
                    ))}
                  </div>
                </>
              )}

              {error && (
                <p className="text-error text-label-md bg-error-container dark:bg-error/20 px-3 py-2 rounded-lg flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px]">error</span>{error}
                </p>
              )}

              {/* Navigation buttons */}
              <div className="flex gap-3 pt-1">
                {step > 0 && (
                  <button
                    type="button"
                    onClick={() => { setStep(s => s - 1); setError('') }}
                    className="flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl font-bold text-sm hover:bg-surface-container dark:hover:bg-slate-800 transition-colors flex items-center justify-center gap-1"
                  >
                    <span className="material-symbols-outlined text-[18px]">arrow_back</span>이전
                  </button>
                )}

                {step < 2 ? (
                  <button
                    type="button"
                    disabled={!canNext()}
                    onClick={() => setStep(s => s + 1)}
                    className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-1"
                  >
                    다음<span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={loading || !form.password || form.password !== form.passwordConfirm}
                    onClick={handleSubmit}
                    className="flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {loading ? '처리 중...' : '가입 완료'}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {/* 로그인 링크 */}
        {!done && (
          <p className="text-center text-sm text-on-surface-variant dark:text-slate-400 mt-6">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-primary dark:text-secondary-fixed font-bold hover:underline">
              로그인
            </Link>
          </p>
        )}
      </div>
    </div>
  )
}
