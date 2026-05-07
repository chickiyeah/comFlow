import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { submitSuggestion } from '../../api/suggestion'

const CATEGORIES = ['ACADEMIC', 'FACILITY', 'WELFARE', 'CURRICULUM', 'GENERAL']

export default function SuggestionFab() {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState({ category: 'GENERAL', content: '' })
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await submitSuggestion(form)
      setSuccess(true)
      setForm({ category: 'GENERAL', content: '' })
      setTimeout(() => { setSuccess(false); setOpen(false) }, 2500)
    } catch {
      setError('제출 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* FAB button */}
      <button
        onClick={() => { setOpen(true); setSuccess(false); setError('') }}
        className="fixed bottom-20 lg:bottom-8 right-6 lg:right-8 w-14 h-14 bg-secondary-fixed text-primary dark:text-[#0f172a] rounded-full shadow-2xl flex items-center justify-center hover:scale-110 active:scale-95 transition-transform z-40"
        title={t('suggestion.title')}
      >
        <span className="material-symbols-outlined text-2xl icon-fill">chat_bubble</span>
      </button>

      {/* Modal */}
      {open && (
        <div
          className="fixed inset-0 bg-black/50 dark:bg-black/70 z-50 flex items-end sm:items-center justify-center p-4"
          onClick={e => { if (e.target === e.currentTarget) setOpen(false) }}
        >
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md shadow-2xl">
            {/* Header */}
            <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="font-['Space_Grotesk'] text-lg font-bold text-primary dark:text-white flex items-center gap-2">
                  <span className="material-symbols-outlined text-secondary-fixed">lock</span>
                  {t('suggestion.title')}
                </h3>
                <p className="text-label-md text-outline dark:text-slate-400 mt-0.5">{t('suggestion.subtitle')}</p>
              </div>
              <button
                onClick={() => setOpen(false)}
                className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors"
              >
                <span className="material-symbols-outlined text-outline dark:text-slate-400">close</span>
              </button>
            </div>

            {/* Body */}
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {success ? (
                <div className="py-8 text-center">
                  <span className="material-symbols-outlined text-[56px] text-secondary dark:text-secondary-fixed">check_circle</span>
                  <p className="mt-3 font-bold text-primary dark:text-white">{t('suggestion.successMsg')}</p>
                </div>
              ) : (
                <>
                  {/* Category */}
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">
                      {t('suggestion.category')}
                    </label>
                    <div className="flex flex-wrap gap-2">
                      {CATEGORIES.map(cat => (
                        <button
                          key={cat}
                          type="button"
                          onClick={() => setForm(f => ({ ...f, category: cat }))}
                          className={`px-3 py-1.5 rounded-full text-label-md font-bold transition-all ${
                            form.category === cat
                              ? 'bg-primary dark:bg-primary-container text-white'
                              : 'bg-surface-container-low dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 hover:bg-surface-container dark:hover:bg-slate-700'
                          }`}
                        >
                          {t(`suggestion.cat_${cat}`)}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Content */}
                  <div>
                    <label className="text-label-md text-on-surface-variant dark:text-slate-400 block mb-2">
                      {t('suggestion.content')}
                    </label>
                    <textarea
                      value={form.content}
                      onChange={e => setForm(f => ({ ...f, content: e.target.value }))}
                      placeholder={t('suggestion.contentPlaceholder')}
                      rows={5}
                      minLength={10}
                      maxLength={1000}
                      required
                      className="w-full px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 dark:focus:ring-secondary-fixed/30 resize-none transition-all"
                    />
                    <p className="text-right text-label-md text-outline dark:text-slate-500 mt-1">
                      {form.content.length} / 1000
                    </p>
                  </div>

                  {error && (
                    <p className="text-error text-label-md bg-error-container dark:bg-error/20 px-3 py-2 rounded-lg">{error}</p>
                  )}

                  <div className="flex items-center gap-3 p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                    <span className="material-symbols-outlined text-outline dark:text-slate-400 text-[18px]">visibility_off</span>
                    <p className="text-label-md text-outline dark:text-slate-400">
                      IP 주소, 학번 등 개인정보는 저장되지 않습니다.
                    </p>
                  </div>

                  <button
                    type="submit"
                    disabled={loading || form.content.length < 10}
                    className="w-full py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-95 transition-transform disabled:opacity-50"
                  >
                    {loading ? '제출 중...' : t('suggestion.submitBtn')}
                  </button>
                </>
              )}
            </form>
          </div>
        </div>
      )}
    </>
  )
}
