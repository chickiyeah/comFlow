import { useState } from 'react'
import { analyzeStudent } from '../../api/assistant'
import useAuthStore from '../../store/authStore'

const PRIORITY_STYLE = {
  '긴급': 'bg-error-container dark:bg-error/20 text-error border-error/30',
  '권장': 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed border-secondary-fixed/30',
  '선택': 'bg-surface-container dark:bg-slate-800 text-on-surface-variant dark:text-slate-300 border-outline-variant',
}

export default function AssistantPanel() {
  const user = useAuthStore(s => s.user)
  const [open, setOpen] = useState(false)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleOpen = async () => {
    setOpen(true)
    if (data) return
    setLoading(true)
    setError('')
    try {
      const res = await analyzeStudent()
      setData(res.data)
    } catch {
      setError('분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setData(null)
    setLoading(true)
    setError('')
    try {
      const res = await analyzeStudent()
      setData(res.data)
    } catch {
      setError('분석 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* 트리거 버튼 — SideNav 하단 / 모바일 하단 */}
      <button
        onClick={handleOpen}
        className="fixed bottom-20 lg:bottom-8 left-24 lg:left-24 w-14 h-14
          bg-primary dark:bg-primary-container text-secondary-fixed
          rounded-full shadow-2xl shadow-primary/30 flex items-center justify-center
          hover:scale-110 active:scale-95 transition-transform z-40
          animate-pulse hover:animate-none"
        title="AI 어시스턴트"
      >
        <span className="material-symbols-outlined text-2xl icon-fill">psychology</span>
      </button>

      {/* 오버레이 */}
      {open && (
        <div
          className="fixed inset-0 bg-black/40 dark:bg-black/60 z-50"
          onClick={() => setOpen(false)}
        />
      )}

      {/* 슬라이드 패널 */}
      <div className={`fixed top-0 right-0 h-full w-full sm:w-[480px] z-50
        bg-white dark:bg-slate-900 shadow-2xl
        transform transition-transform duration-300 ease-in-out overflow-y-auto
        ${open ? 'translate-x-0' : 'translate-x-full'}`}>

        {/* 헤더 */}
        <div className="sticky top-0 bg-white dark:bg-slate-900 border-b border-slate-100 dark:border-slate-800 p-5 flex items-center justify-between z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary dark:bg-primary-container flex items-center justify-center">
              <span className="material-symbols-outlined text-secondary-fixed icon-fill">psychology</span>
            </div>
            <div>
              <h2 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white">AI 학생 어시스턴트</h2>
              <p className="text-label-md text-outline dark:text-slate-400">{user?.name}님 맞춤 분석</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {data && (
              <button onClick={handleRefresh} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400" title="새로고침">
                <span className="material-symbols-outlined text-[20px]">refresh</span>
              </button>
            )}
            <button onClick={() => setOpen(false)} className="p-2 rounded-full hover:bg-surface-container dark:hover:bg-slate-800 transition-colors text-outline dark:text-slate-400">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        </div>

        <div className="p-5 space-y-4">

          {/* 로딩 */}
          {loading && (
            <div className="space-y-4">
              <div className="flex flex-col items-center py-12 gap-4">
                <div className="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
                <p className="text-sm text-on-surface-variant dark:text-slate-400 text-center">
                  학업 데이터를 분석하고 있습니다...<br />
                  <span className="text-label-md">성적·출결·졸업요건을 종합 검토 중</span>
                </p>
              </div>
              {[1,2,3].map(i => (
                <div key={i} className="animate-pulse bg-surface-container-low dark:bg-slate-800 rounded-2xl p-5 h-24" />
              ))}
            </div>
          )}

          {/* 에러 */}
          {!loading && error && (
            <div className="p-4 bg-error-container dark:bg-error/20 text-error rounded-xl flex items-center gap-3">
              <span className="material-symbols-outlined">error</span>
              <p className="text-sm">{error}</p>
            </div>
          )}

          {/* 결과 */}
          {!loading && data && (
            <>
              {/* 학생 요약 */}
              <div className="bg-primary dark:bg-primary-container rounded-2xl p-5 text-white relative overflow-hidden">
                <div className="absolute -bottom-4 -right-4 opacity-10">
                  <span className="material-symbols-outlined text-[100px]">school</span>
                </div>
                <div className="relative z-10">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <p className="text-secondary-fixed font-bold text-lg font-['Space_Grotesk']">{data.studentName}</p>
                      <p className="text-white/70 text-sm">{data.currentSemester}학기 · 남은 학기 {data.remainingSemesters}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-secondary-fixed font-black text-2xl font-['Space_Grotesk']">{data.gpa.toFixed(2)}</p>
                      <p className="text-white/70 text-xs">/ 4.5 GPA</p>
                    </div>
                  </div>
                  <div className="bg-white/10 rounded-xl p-3">
                    <p className="text-sm text-white leading-relaxed">{data.overallAdvice}</p>
                  </div>
                </div>
              </div>

              {/* 출결 경고 */}
              {data.attendanceWarnings?.length > 0 && (
                <div className="bg-error-container dark:bg-error/20 rounded-2xl p-4 border border-error/30">
                  <h3 className="font-bold text-error flex items-center gap-2 mb-2">
                    <span className="material-symbols-outlined text-[18px]">warning</span>출결 경고
                  </h3>
                  {data.attendanceWarnings.map((w, i) => (
                    <p key={i} className="text-sm text-error/80 flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-error" />{w}
                    </p>
                  ))}
                </div>
              )}

              {/* 자격증 추천 */}
              {data.certificates?.length > 0 && (
                <div className="card p-5">
                  <h3 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">workspace_premium</span>자격증 추천
                  </h3>
                  <div className="space-y-2.5">
                    {data.certificates.map((c, i) => (
                      <div key={i} className={`p-3 rounded-xl border ${PRIORITY_STYLE[c.priority] ?? PRIORITY_STYLE['권장']}`}>
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-bold text-sm">{c.name}</span>
                          <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-white/40 dark:bg-black/20">{c.priority}</span>
                        </div>
                        <p className="text-xs opacity-80">{c.reason}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 다음 학기 집중 과목 */}
              {data.studyFocus?.length > 0 && (
                <div className="card p-5">
                  <h3 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">menu_book</span>다음 학기 집중 과목
                  </h3>
                  <div className="space-y-2">
                    {data.studyFocus.map((s, i) => (
                      <div key={i} className="flex items-start gap-2.5 p-2.5 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                        <span className="w-6 h-6 rounded-full bg-primary dark:bg-primary-container text-white flex items-center justify-center text-xs font-black shrink-0 mt-0.5">{i + 1}</span>
                        <p className="text-sm text-on-surface dark:text-slate-300">{s}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 포트폴리오 아이디어 */}
              {data.portfolioIdeas?.length > 0 && (
                <div className="card p-5">
                  <h3 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">lightbulb</span>추천 포트폴리오 주제
                  </h3>
                  <div className="space-y-2">
                    {data.portfolioIdeas.map((idea, i) => (
                      <div key={i} className="flex items-start gap-2.5 p-3 bg-secondary-container/20 dark:bg-secondary-fixed/10 rounded-xl border border-secondary-fixed/20">
                        <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed text-[16px] mt-0.5 shrink-0">arrow_right</span>
                        <p className="text-sm text-on-surface dark:text-slate-300">{idea}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 졸업요건 경고 */}
              {data.graduationWarnings?.filter(w => w.shortage > 0).length > 0 && (
                <div className="card p-5">
                  <h3 className="font-['Space_Grotesk'] font-bold text-primary dark:text-white mb-3 flex items-center gap-2">
                    <span className="material-symbols-outlined text-secondary dark:text-secondary-fixed">school</span>졸업요건 현황
                  </h3>
                  <div className="space-y-2">
                    {data.graduationWarnings.map((w, i) => (
                      <div key={i} className="flex items-center justify-between p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                        <div>
                          <p className="font-bold text-sm text-primary dark:text-white">{w.category}</p>
                        </div>
                        <div className="text-right">
                          <p className="font-bold text-sm text-error">{w.shortage}학점 부족</p>
                          <div className="w-20 h-1.5 bg-surface-container dark:bg-slate-700 rounded-full mt-1">
                            <div className="h-full bg-error rounded-full" style={{ width: `${Math.max(5, Math.min(100, (w.earned / w.required) * 100))}%` }} />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <p className="text-center text-label-md text-outline dark:text-slate-500 pb-4">
                AI 분석 결과는 참고용입니다. 정확한 상담은 담당 교수님께 문의하세요.
              </p>
            </>
          )}
        </div>
      </div>
    </>
  )
}
