import { useState, useEffect } from 'react'
import Layout from '../components/layout/Layout'
import { getCalendarEvents } from '../api/calendar'

const TYPE_COLOR = {
  LECTURE:   { bg: 'bg-primary/10 dark:bg-primary/20',   text: 'text-primary dark:text-blue-300',   label: '강의' },
  NOTICE:    { bg: 'bg-red-50 dark:bg-red-900/20',       text: 'text-red-600 dark:text-red-400',    label: '공지' },
  CERT_EXAM: { bg: 'bg-yellow-50 dark:bg-yellow-900/20', text: 'text-yellow-700 dark:text-yellow-400', label: '자격증' },
}

export default function Calendar() {
  const now = new Date()
  const [year, setYear]   = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    getCalendarEvents(year, month)
      .then(r => setEvents(r.data || []))
      .catch(() => setEvents([]))
      .finally(() => setLoading(false))
  }, [year, month])

  const prevMonth = () => { if (month === 1) { setYear(y => y-1); setMonth(12) } else setMonth(m => m-1) }
  const nextMonth = () => { if (month === 12) { setYear(y => y+1); setMonth(1) } else setMonth(m => m+1) }

  // 날짜별 그룹화
  const byDate = events.reduce((acc, e) => {
    const d = e.date
    if (!acc[d]) acc[d] = []
    acc[d].push(e)
    return acc
  }, {})

  // 달력 그리드
  const firstDay = new Date(year, month-1, 1).getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const cells = []
  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(d)

  const todayStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`

  return (
    <Layout>
      <div className="max-w-4xl mx-auto">
        {/* 헤더 */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white flex items-center gap-2">
            <span className="material-symbols-outlined text-secondary-fixed">calendar_month</span>
            통합 캘린더
          </h1>
          <div className="flex items-center gap-3">
            <button onClick={prevMonth} className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
              <span className="material-symbols-outlined">chevron_left</span>
            </button>
            <span className="font-bold text-primary dark:text-white text-lg min-w-[120px] text-center">
              {year}년 {month}월
            </span>
            <button onClick={nextMonth} className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
              <span className="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>

        {/* 범례 */}
        <div className="flex gap-4 mb-4 text-xs">
          {Object.entries(TYPE_COLOR).map(([k, v]) => (
            <span key={k} className={`px-2 py-1 rounded-full font-medium ${v.bg} ${v.text}`}>{v.label}</span>
          ))}
        </div>

        {/* 달력 */}
        <div className="bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 overflow-hidden shadow-sm">
          <div className="grid grid-cols-7 border-b border-slate-100 dark:border-slate-800">
            {['일','월','화','수','목','금','토'].map((d,i) => (
              <div key={d} className={`py-2 text-center text-xs font-bold ${i===0?'text-red-500':i===6?'text-blue-500':'text-outline dark:text-slate-400'}`}>{d}</div>
            ))}
          </div>
          {loading ? (
            <div className="py-20 text-center text-outline dark:text-slate-500">불러오는 중…</div>
          ) : (
            <div className="grid grid-cols-7">
              {cells.map((day, idx) => {
                if (!day) return <div key={`empty-${idx}`} className="min-h-[80px] border-b border-r border-slate-50 dark:border-slate-800/50" />
                const dateStr = `${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`
                const dayEvents = byDate[dateStr] || []
                const isToday = dateStr === todayStr
                const dow = (idx) % 7
                return (
                  <div key={day} className={`min-h-[80px] border-b border-r border-slate-50 dark:border-slate-800/50 p-1 ${isToday ? 'bg-secondary-container/30 dark:bg-secondary-fixed/10' : ''}`}>
                    <span className={`text-xs font-bold inline-block w-6 h-6 flex items-center justify-center rounded-full mb-1
                      ${isToday ? 'bg-secondary-fixed text-primary' : dow===0?'text-red-500':dow===6?'text-blue-500':'text-slate-700 dark:text-slate-300'}`}>
                      {day}
                    </span>
                    <div className="space-y-0.5">
                      {dayEvents.slice(0, 3).map((e, i) => {
                        const style = TYPE_COLOR[e.type] || TYPE_COLOR.LECTURE
                        return (
                          <div key={i} className={`text-[9px] px-1 py-0.5 rounded truncate font-medium ${style.bg} ${style.text}`}
                               title={`${e.title}\n${e.description}`}>
                            {e.title}
                          </div>
                        )
                      })}
                      {dayEvents.length > 3 && (
                        <div className="text-[9px] text-outline dark:text-slate-500">+{dayEvents.length-3}개</div>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* 이번 달 일정 리스트 */}
        {events.length > 0 && (
          <div className="mt-6">
            <h2 className="font-bold text-primary dark:text-white mb-3">{month}월 전체 일정</h2>
            <div className="space-y-2">
              {Object.entries(byDate).sort().map(([date, evs]) => (
                <div key={date} className="bg-white dark:bg-slate-900 rounded-xl border border-slate-100 dark:border-slate-800 p-4">
                  <p className="text-sm font-bold text-primary dark:text-white mb-2">
                    {new Date(date).toLocaleDateString('ko', {month:'long',day:'numeric',weekday:'short'})}
                  </p>
                  <div className="space-y-1">
                    {evs.map((e, i) => {
                      const style = TYPE_COLOR[e.type] || TYPE_COLOR.LECTURE
                      return (
                        <div key={i} className={`flex items-center gap-2 px-3 py-1.5 rounded-lg ${style.bg}`}>
                          <span className={`text-xs font-bold ${style.text}`}>{TYPE_COLOR[e.type]?.label}</span>
                          <span className={`text-sm ${style.text}`}>{e.title}</span>
                          {e.description && <span className="text-xs text-outline dark:text-slate-500 ml-auto">{e.description}</span>}
                        </div>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}
