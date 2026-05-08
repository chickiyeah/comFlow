import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register, sendEmailCode, verifyEmailCode } from '../../api/auth'
import useThemeStore from '../../store/themeStore'
import LanguagePicker from '../../components/common/LanguagePicker'

const STEP = { ROLE: 0, INFO: 1, EMAIL: 2, PASSWORD: 3 }
const TOTAL_STEPS = 4

const INPUT_CLS = 'w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all'
const BTN_PRIMARY = 'flex-1 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 disabled:cursor-not-allowed'
const BTN_OUTLINE = 'flex-1 py-3 border border-outline-variant dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl font-bold text-sm hover:bg-surface-container dark:hover:bg-slate-800 transition-colors flex items-center justify-center gap-1'

export default function Register() {
  const navigate = useNavigate()
  const { dark, toggle } = useThemeStore()

  const [step, setStep] = useState(STEP.ROLE)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)

  const [form, setForm] = useState({
    role: '', username: '', name: '',
    email: '', password: '', passwordConfirm: '',
  })

  const [codeSent, setCodeSent] = useState(false)
  const [codeInput, setCodeInput] = useState('')
  const [verified, setVerified] = useState(false)
  const [sendLoading, setSendLoading] = useState(false)
  const [verifyLoading, setVerifyLoading] = useState(false)

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }))
  const clearError = () => setError('')

  const canNext = () => {
    if (step === STEP.ROLE) return !!form.role
    if (step === STEP.INFO) return form.username.trim().length >= 4 && form.name.trim().length >= 2
    if (step === STEP.EMAIL) return verified
    return false
  }

  const handleSendCode = async () => {
    if (!form.email || !/\S+@\S+\.\S+/.test(form.email)) {
      setError('올바른 이메일을 입력해주세요.'); return
    }
    setSendLoading(true); clearError()
    try {
      await sendEmailCode(form.email)
      setCodeSent(true); setVerified(false); setCodeInput('')
    } catch (err) {
      setError(err?.message || '인증 코드 발송에 실패했습니다.')
    } finally {
      setSendLoading(false)
    }
  }

  const handleVerifyCode = async () => {
    if (codeInput.length !== 6) { setError('6자리 코드를 입력해주세요.'); return }
    setVerifyLoading(true); clearError()
    try {
      await verifyEmailCode(form.email, codeInput)
      setVerified(true)
    } catch (err) {
      setError(err?.message || '인증 코드가 올바르지 않습니다.')
    } finally {
      setVerifyLoading(false)
    }
  }

  const handleSubmit = async () => {
    if (form.password.length < 6) { setError('비밀번호는 6자 이상이어야 합니다.'); return }
    if (form.password !== form.passwordConfirm) { setError('비밀번호가 일치하지 않습니다.'); return }
    setLoading(true); clearError()
    try {
      await register({
        username: form.username.trim(),
        password: form.password,
        name: form.name.trim(),
        email: form.email.trim(),
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

      <div className="fixed top-4 right-4 flex items-center gap-2 z-50">
        <LanguagePicker />
        <button onClick={toggle} className="p-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:scale-110 transition-transform">
          <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
        </button>
      </div>

      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary dark:bg-primary-container mb-3">
            <span className="text-secondary-fixed font-black text-xl font-['Space_Grotesk']">CF</span>
          </div>
          <h1 className="text-2xl font-black text-primary dark:text-white font-['Space_Grotesk']">회원가입</h1>
          <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">CampusFlow 계정을 만들어보세요.</p>
        </div>

        {!done && (
          <div className="flex items-center justify-center gap-2 mb-6">
            {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
              <div key={i} className={`h-1.5 rounded-full transition-all duration-300 ${
                i <= step ? 'bg-primary dark:bg-secondary-fixed' : 'bg-outline-variant dark:bg-slate-700'
              } ${i === step ? 'w-8' : 'w-4'}`} />
            ))}
          </div>
        )}

        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 p-8 shadow-sm">

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

              {/* Step 0: 구분 선택 */}
              {step === STEP.ROLE && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">구분 선택</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-4">학생 또는 교수 / 교직원으로 가입합니다.</p>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'ROLE_STUDENT',   icon: 'school',      label: '학생',        sub: '학번으로 가입' },
                      { value: 'ROLE_PROFESSOR',  icon: 'person_book', label: '교수/교직원', sub: '교번으로 가입' },
                    ].map(r => (
                      <button key={r.value} type="button" onClick={() => set('role', r.value)}
                        className={`p-5 rounded-2xl border-2 flex flex-col items-center gap-2 transition-all active:scale-95 ${
                          form.role === r.value
                            ? 'border-primary dark:border-secondary-fixed bg-primary/5 dark:bg-secondary-fixed/10'
                            : 'border-outline-variant dark:border-slate-700 hover:border-primary/50'
                        }`}>
                        <span className={`material-symbols-outlined text-3xl ${form.role === r.value ? 'text-primary dark:text-secondary-fixed' : 'text-outline dark:text-slate-400'}`}>{r.icon}</span>
                        <span className={`font-bold text-sm ${form.role === r.value ? 'text-primary dark:text-white' : 'text-on-surface dark:text-slate-300'}`}>{r.label}</span>
                        <span className="text-[10px] text-on-surface-variant dark:text-slate-500">{r.sub}</span>
                        {form.role === r.value && <span className="material-symbols-outlined text-[16px] text-primary dark:text-secondary-fixed">check_circle</span>}
                      </button>
                    ))}
                  </div>
                </>
              )}

              {/* Step 1: 기본 정보 */}
              {step === STEP.INFO && (
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
                    <input autoFocus type="text" value={form.username}
                      onChange={e => set('username', e.target.value)}
                      placeholder={form.role === 'ROLE_STUDENT' ? '예: 2024010001' : '예: P2024001'}
                      className={INPUT_CLS} />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">이름</label>
                    <input type="text" value={form.name}
                      onChange={e => set('name', e.target.value)}
                      placeholder="실명을 입력해주세요" className={INPUT_CLS} />
                  </div>
                </>
              )}

              {/* Step 2: 이메일 인증 */}
              {step === STEP.EMAIL && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">이메일 인증</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">학교 이메일로 인증 코드를 받으세요.</p>
                  </div>

                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">이메일</label>
                    <div className="flex gap-2">
                      <input type="email" value={form.email}
                        onChange={e => { set('email', e.target.value); setCodeSent(false); setVerified(false); clearError() }}
                        placeholder="example@school.ac.kr"
                        disabled={verified}
                        className={INPUT_CLS + (verified ? ' opacity-60' : '')} />
                      <button type="button" onClick={handleSendCode}
                        disabled={sendLoading || verified || !form.email}
                        className="px-4 py-3 bg-surface-container dark:bg-slate-700 text-primary dark:text-secondary-fixed rounded-xl text-label-md font-bold hover:bg-surface-container-high dark:hover:bg-slate-600 transition-colors disabled:opacity-40 whitespace-nowrap shrink-0">
                        {sendLoading ? '발송 중...' : codeSent ? '재발송' : '코드 발송'}
                      </button>
                    </div>
                  </div>

                  {codeSent && !verified && (
                    <div>
                      <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">
                        인증 코드 <span className="text-outline dark:text-slate-500">(10분 내 입력)</span>
                      </label>
                      <div className="flex gap-2">
                        <input autoFocus type="text" value={codeInput}
                          onChange={e => { setCodeInput(e.target.value.replace(/\D/g, '').slice(0, 6)); clearError() }}
                          placeholder="6자리 숫자" maxLength={6}
                          className={INPUT_CLS + ' tracking-[0.4em] text-center font-bold text-lg'} />
                        <button type="button" onClick={handleVerifyCode}
                          disabled={verifyLoading || codeInput.length !== 6}
                          className="px-4 py-3 bg-primary dark:bg-primary-container text-white rounded-xl text-label-md font-bold hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-40 whitespace-nowrap shrink-0">
                          {verifyLoading ? '확인 중...' : '인증 확인'}
                        </button>
                      </div>
                    </div>
                  )}

                  {verified && (
                    <div className="flex items-center gap-3 p-4 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-xl border border-secondary-fixed/30">
                      <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-2xl">verified</span>
                      <div>
                        <p className="font-bold text-sm text-primary dark:text-white">이메일 인증 완료</p>
                        <p className="text-label-md text-outline dark:text-slate-400">{form.email}</p>
                      </div>
                    </div>
                  )}
                </>
              )}

              {/* Step 3: 비밀번호 */}
              {step === STEP.PASSWORD && (
                <>
                  <div>
                    <p className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-1">비밀번호 설정</p>
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400">6자 이상의 비밀번호를 설정하세요.</p>
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">비밀번호</label>
                    <input autoFocus type="password" value={form.password}
                      onChange={e => { set('password', e.target.value); clearError() }}
                      placeholder="6자 이상 입력" className={INPUT_CLS} />
                  </div>
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">비밀번호 확인</label>
                    <input type="password" value={form.passwordConfirm}
                      onChange={e => { set('passwordConfirm', e.target.value); clearError() }}
                      placeholder="동일한 비밀번호 재입력"
                      className={INPUT_CLS + (form.passwordConfirm && form.password !== form.passwordConfirm ? ' border-error' : '')} />
                    {form.passwordConfirm && form.password !== form.passwordConfirm && (
                      <p className="text-[11px] text-error mt-1">비밀번호가 일치하지 않습니다.</p>
                    )}
                    {form.passwordConfirm && form.password === form.passwordConfirm && (
                      <p className="text-[11px] text-secondary dark:text-secondary-fixed mt-1 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">check_circle</span>비밀번호가 일치합니다.
                      </p>
                    )}
                  </div>

                  <div className="p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl border border-outline-variant dark:border-slate-700 space-y-1.5">
                    <p className="text-label-md text-on-surface-variant dark:text-slate-400 mb-1.5">입력 정보 확인</p>
                    {[
                      { label: '구분',   val: form.role === 'ROLE_STUDENT' ? '학생' : '교수/교직원' },
                      { label: form.role === 'ROLE_STUDENT' ? '학번' : '교번', val: form.username },
                      { label: '이름',   val: form.name },
                      { label: '이메일', val: form.email },
                    ].map(({ label, val }) => (
                      <div key={label} className="flex justify-between text-sm">
                        <span className="text-on-surface-variant dark:text-slate-400">{label}</span>
                        <span className="font-medium text-primary dark:text-white">{val}</span>
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

              <div className="flex gap-3 pt-1">
                {step > STEP.ROLE && (
                  <button type="button" onClick={() => { setStep(s => s - 1); clearError() }} className={BTN_OUTLINE}>
                    <span className="material-symbols-outlined text-[18px]">arrow_back</span>이전
                  </button>
                )}
                {step < STEP.PASSWORD ? (
                  <button type="button" disabled={!canNext()} onClick={() => { setStep(s => s + 1); clearError() }}
                    className={BTN_PRIMARY + ' flex items-center justify-center gap-1'}>
                    다음<span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </button>
                ) : (
                  <button type="button" onClick={handleSubmit}
                    disabled={loading || !form.password || form.password !== form.passwordConfirm}
                    className={BTN_PRIMARY}>
                    {loading ? '처리 중...' : '가입 완료'}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {!done && (
          <p className="text-center text-sm text-on-surface-variant dark:text-slate-400 mt-6">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-primary dark:text-secondary-fixed font-bold hover:underline">로그인</Link>
          </p>
        )}
      </div>
    </div>
  )
}
