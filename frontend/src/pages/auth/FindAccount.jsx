import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { findUsername, sendEmailCode, resetPassword } from '../../api/auth'
import useThemeStore from '../../store/themeStore'
import LanguagePicker from '../../components/common/LanguagePicker'

export default function FindAccount() {
  const navigate = useNavigate()
  const { dark, toggle } = useThemeStore()
  const { t } = useTranslation()
  const [tab, setTab] = useState('id') // 'id' | 'pw'

  return (
    <div className="min-h-screen bg-background dark:bg-[#0b0e14] flex items-center justify-center p-4 transition-colors duration-300">
      <div className="fixed top-4 right-4 flex items-center gap-2">
        <LanguagePicker />
        <button onClick={toggle} className="p-2 rounded-full bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-400 hover:scale-110 transition-transform">
          <span className="material-symbols-outlined">{dark ? 'light_mode' : 'dark_mode'}</span>
        </button>
      </div>

      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary dark:bg-primary-container mb-4">
            <span className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">CF</span>
          </div>
          <h1 className="text-xl font-black text-primary dark:text-white font-['Space_Grotesk']">{t('auth.findAccount')}</h1>
        </div>

        {/* 탭 */}
        <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl mb-5">
          {[['id', t('auth.findId')], ['pw', t('auth.resetPw')]].map(([key, label]) => (
            <button key={key} onClick={() => setTab(key)}
              className={`flex-1 py-2.5 rounded-xl text-sm font-bold transition-all ${
                tab === key
                  ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm'
                  : 'text-on-surface-variant dark:text-slate-400'
              }`}>{label}</button>
          ))}
        </div>

        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-outline-variant dark:border-slate-800 p-8 shadow-sm">
          {tab === 'id' ? <FindIdPanel /> : <ResetPwPanel />}
        </div>

        <p className="text-center text-sm text-on-surface-variant dark:text-slate-400 mt-6">
          <Link to="/login" className="text-primary dark:text-secondary-fixed font-bold hover:underline">{t('auth.backToLogin')}</Link>
        </p>
      </div>
    </div>
  )
}

function Field({ label, ...props }) {
  return (
    <div>
      <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{label}</label>
      <input {...props}
        className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 transition-all" />
    </div>
  )
}

function FindIdPanel() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [result, setResult] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setResult(''); setLoading(true)
    try {
      const res = await findUsername(email.trim())
      setResult(res.data.username)
    } catch {
      setError(t('auth.accountNotFound'))
    } finally { setLoading(false) }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('auth.findIdDesc')}</p>
      <Field label={t('auth.email')} type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="example@school.ac.kr" required />
      {result && (
        <div className="bg-secondary-container/30 dark:bg-secondary-fixed/10 border border-secondary-fixed/40 rounded-xl px-4 py-3 text-center">
          <p className="text-xs text-on-surface-variant dark:text-slate-400 mb-1">{t('auth.yourIdIs')}</p>
          <p className="text-lg font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk'] tracking-wide">{result}</p>
        </div>
      )}
      {error && <p className="text-error text-label-md bg-error-container dark:bg-error/20 px-3 py-2 rounded-lg">{error}</p>}
      <button type="submit" disabled={loading}
        className="w-full py-3 bg-primary dark:bg-primary-container text-on-primary dark:text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-60">
        {loading ? t('auth.processing') : t('auth.findIdBtn')}
      </button>
    </form>
  )
}

function ResetPwPanel() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPw, setNewPw] = useState('')
  const [sent, setSent] = useState(false)
  const [sending, setSending] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const send = async () => {
    if (!email.trim()) { setError(t('auth.invalidEmail')); return }
    setError(''); setSending(true)
    try { await sendEmailCode(email.trim()); setSent(true) }
    catch { setError(t('auth.codeSendFailed')) }
    finally { setSending(false) }
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (code.trim().length < 6) { setError(t('auth.enterSixDigit')); return }
    if (newPw.length < 4) { setError(t('auth.passwordTooShort')); return }
    setLoading(true)
    try {
      await resetPassword(email.trim(), code.trim(), newPw)
      setSuccess(t('auth.resetPwSuccess'))
      setTimeout(() => navigate('/login'), 1800)
    } catch {
      setError(t('auth.codeInvalid'))
    } finally { setLoading(false) }
  }

  if (success) {
    return (
      <div className="text-center py-6">
        <span className="material-symbols-outlined text-[56px] text-secondary dark:text-secondary-fixed">check_circle</span>
        <p className="mt-3 font-bold text-primary dark:text-white">{success}</p>
      </div>
    )
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm text-on-surface-variant dark:text-slate-400">{t('auth.resetPwDesc')}</p>
      <div>
        <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-1.5">{t('auth.email')}</label>
        <div className="flex gap-2">
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="example@school.ac.kr" required
            className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
          <button type="button" onClick={send} disabled={sending}
            className="px-4 py-3 bg-surface-container dark:bg-slate-700 text-primary dark:text-white rounded-xl text-sm font-bold whitespace-nowrap disabled:opacity-60">
            {sending ? t('auth.sending') : (sent ? t('auth.resend') : t('auth.sendCode'))}
          </button>
        </div>
      </div>
      {sent && (
        <>
          <Field label={`${t('auth.verifyCode')} ${t('auth.codeExpiry')}`} value={code} onChange={e => setCode(e.target.value)} placeholder={t('auth.sixDigitPlaceholder')} maxLength={6} required />
          <Field label={t('auth.newPassword')} type="password" value={newPw} onChange={e => setNewPw(e.target.value)} placeholder={t('auth.newPasswordPlaceholder')} required />
        </>
      )}
      {error && <p className="text-error text-label-md bg-error-container dark:bg-error/20 px-3 py-2 rounded-lg">{error}</p>}
      <button type="submit" disabled={loading || !sent}
        className="w-full py-3 bg-primary dark:bg-primary-container text-on-primary dark:text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-60">
        {loading ? t('auth.processing') : t('auth.resetPwBtn')}
      </button>
    </form>
  )
}
