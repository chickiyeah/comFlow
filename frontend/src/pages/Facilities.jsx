import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { searchBooks } from '../api/library'
import { getPortalShuttle } from '../api/portal'
import { getFacilityStats } from '../api/facilities'
import { getProfile } from '../api/profile'

const BOOK_CATEGORIES = ['IT/컴퓨터', '수학/통계', '영어', '취업/자격증', '교양', '소설']

const AMENITIES = [
  { icon: 'fitness_center', label: '피트니스'  },
  { icon: 'restaurant',     label: '학식 식당' },
  { icon: 'local_cafe',     label: '카페테리아'},
  { icon: 'print',          label: '복사실'    },
  { icon: 'local_library',  label: '도서관'    },
  { icon: 'sports_soccer',  label: '체육관'    },
]

export default function Facilities() {
  const { t } = useTranslation()
  const [facTab, setFacTab] = useState('campus')

  // 도서 검색
  const [bookKeyword, setBookKeyword] = useState('')
  const [bookCategory, setBookCategory] = useState('')
  const [books, setBooks]       = useState([])
  const [bookLoading, setBookLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  // 통학버스
  const [shuttleData, setShuttleData]       = useState(null)
  const [shuttleLoading, setShuttleLoading] = useState(false)
  const [shuttleError, setShuttleError]     = useState(false)
  const [portalSynced, setPortalSynced]     = useState(false)

  // 시설 통계
  const [stats, setStats] = useState({})
  const [statsLoading, setStatsLoading] = useState(true)

  useEffect(() => {
    setShuttleLoading(true)
    getPortalShuttle()
      .then(r => setShuttleData(r.data))
      .catch(() => setShuttleError(true))
      .finally(() => setShuttleLoading(false))

    getProfile().then(r => { if (r.data?.intranetSyncEnabled) setPortalSynced(true) }).catch(() => {})

    getFacilityStats()
      .then(r => {
        const map = {}
        ;(r.data || []).forEach(s => { map[s.statKey] = s })
        setStats(map)
      })
      .catch(() => {})
      .finally(() => setStatsLoading(false))
  }, [])

  const handleBookSearch = async (e) => {
    e.preventDefault()
    setBookLoading(true)
    setSearched(true)
    try {
      const r = await searchBooks(bookKeyword, bookCategory)
      setBooks(r.data?.content ?? [])
    } catch {
      setBooks([])
    } finally {
      setBookLoading(false)
    }
  }

  const sv = (key, fallback = '—') => stats[key]?.value ?? fallback
  const su = (key) => stats[key]?.unit ?? ''

  const libAvail = parseInt(sv('library_available', '0'), 10)
  const libTotal = parseInt(sv('library_total', '120'), 10)
  const libPct   = libTotal > 0 ? Math.round((libAvail / libTotal) * 100) : 0

  const FAC_TABS = [
    { key: 'campus', label: '캠퍼스 · 기숙사' },
    { key: 'books',  label: t('facilities.tab_books') },
  ]

  return (
    <Layout title={t('facilities.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('facilities.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('facilities.subtitle')}</p>
      </div>

      {/* 탭 */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {FAC_TABS.map(t2 => (
          <button key={t2.key} onClick={() => setFacTab(t2.key)}
            className={`px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
              facTab === t2.key
                ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm'
                : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}>{t2.label}</button>
        ))}
      </div>

      {/* ── 도서 검색 탭 ── */}
      {facTab === 'books' && (
        <div>
          <form onSubmit={handleBookSearch} className="card p-5 mb-6 flex flex-col sm:flex-row gap-3">
            <div className="flex-1 flex gap-2">
              <input value={bookKeyword} onChange={e => setBookKeyword(e.target.value)}
                placeholder={t('facilities.bookSearchPlaceholder')}
                className="flex-1 px-4 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30" />
              <select value={bookCategory} onChange={e => setBookCategory(e.target.value)}
                className="px-3 py-3 bg-surface-container-low dark:bg-slate-800 border border-outline-variant dark:border-slate-700 dark:text-on-surface rounded-xl text-sm focus:outline-none hidden sm:block">
                <option value="">{t('facilities.allCategory')}</option>
                {BOOK_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <button type="submit" className="px-6 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm hover:scale-[1.02] active:scale-95 transition-transform">
              {t('common.search')}
            </button>
          </form>

          {bookLoading && <div className="flex justify-center py-12"><div className="w-8 h-8 border-4 border-primary/20 border-t-primary rounded-full animate-spin"/></div>}
          {!bookLoading && searched && books.length === 0 && (
            <div className="card p-12 text-center">
              <span className="material-symbols-outlined text-[56px] text-outline dark:text-slate-600">menu_book</span>
              <p className="mt-3 font-bold text-primary dark:text-white">검색 결과가 없습니다.</p>
            </div>
          )}
          {!bookLoading && books.length > 0 && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {books.map(b => (
                <div key={b.id} className="card p-5">
                  <div className="flex items-start justify-between mb-2">
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${b.available ? 'bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed' : 'bg-error-container dark:bg-error/20 text-error'}`}>
                      {b.available ? t('facilities.available') : t('facilities.unavailable')}
                    </span>
                    {b.category && <span className="text-label-md text-outline dark:text-slate-500">{b.category}</span>}
                  </div>
                  <h4 className="font-bold text-primary dark:text-white mb-1 line-clamp-2">{b.title}</h4>
                  <p className="text-sm text-on-surface-variant dark:text-slate-400">{b.author}</p>
                  {b.publisher && <p className="text-label-md text-outline dark:text-slate-500">{b.publisher} · {b.publishYear}</p>}
                  <div className="mt-3 flex items-center justify-between">
                    <span className="text-label-md text-outline dark:text-slate-500">청구기호 {b.callNumber}</span>
                    <span className="text-label-md font-bold text-primary dark:text-secondary-fixed">{b.availableCopies}/{b.totalCopies}권</span>
                  </div>
                </div>
              ))}
            </div>
          )}
          {!searched && (
            <div className="card p-16 text-center">
              <span className="material-symbols-outlined text-[64px] text-outline dark:text-slate-600">local_library</span>
              <p className="mt-3 font-bold text-primary dark:text-white font-['Space_Grotesk']">{t('facilities.bookSearch')}</p>
              <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-2">{t('facilities.bookSearchPlaceholder')}</p>
            </div>
          )}
        </div>
      )}

      {/* ── 캠퍼스·기숙사 탭 ── */}
      {facTab === 'campus' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-card_gap">

          {/* 통학버스 */}
          <div className="lg:col-span-2 card p-6">
            <div className="flex items-center justify-between mb-5">
              <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-secondary-container dark:bg-secondary-fixed/20 flex items-center justify-center">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed text-[20px]">directions_bus</span>
                </div>
                통학버스 안내
              </h3>
              <span className="px-2 py-1 bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed text-[10px] font-bold rounded-full">포털 연동</span>
            </div>

            {shuttleLoading && <div className="flex justify-center py-8"><div className="w-6 h-6 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>}
            {!shuttleLoading && shuttleError && (
              <div className="text-center py-8 text-on-surface-variant dark:text-slate-400 text-sm">
                <span className="material-symbols-outlined text-[40px] block mb-2 text-outline dark:text-slate-600">directions_bus</span>
                {portalSynced
                  ? <><p className="font-bold text-primary dark:text-white text-base mb-1">포털 세션이 만료되었습니다</p><p>프로필 → <strong>지금 다시 동기화</strong>를 눌러주세요.</p></>
                  : <p>포털 연동 후 실시간 버스 정보를 확인할 수 있습니다.</p>
                }
              </div>
            )}
            {!shuttleLoading && shuttleData && (() => {
              const routes = shuttleData.ds_out_1 ?? []
              const stops  = shuttleData.ds_out_2 ?? []
              const times  = shuttleData.ds_out_3 ?? []
              return (
                <div>
                  {routes.length > 0 && (
                    <div className="mb-4">
                      <p className="text-label-md text-outline dark:text-slate-400 mb-2">운행 노선</p>
                      <div className="space-y-2">
                        {routes.map((r, i) => (
                          <div key={i} className="flex items-center justify-between p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                            <span className="font-bold text-primary dark:text-white text-sm">{r.bus_route_nm ?? r.nm ?? `노선 ${i+1}`}</span>
                            <span className={`text-label-md font-bold ${r.oper_yn === 'Y' ? 'text-green-600 dark:text-green-400' : 'text-outline dark:text-slate-500'}`}>
                              {r.oper_yn === 'Y' ? '운행중' : '미운행'}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  {stops.length > 0 && (
                    <div className="mb-4">
                      <p className="text-label-md text-outline dark:text-slate-400 mb-2">정류장 ({stops.length}개)</p>
                      <div className="flex gap-2 overflow-x-auto pb-1">
                        {stops.map((s, i) => (
                          <span key={i} className="shrink-0 px-3 py-1 bg-surface-container dark:bg-slate-700 text-on-surface dark:text-slate-300 rounded-full text-xs">
                            {s.stop_nm ?? s.nm ?? `정류장${i+1}`}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {times.length > 0 && (
                    <div>
                      <p className="text-label-md text-outline dark:text-slate-400 mb-2">운행 시간표</p>
                      <div className="flex gap-2 flex-wrap">
                        {times.slice(0,12).map((t2, i) => (
                          <span key={i} className="px-3 py-1.5 bg-secondary-container/30 dark:bg-secondary-fixed/10 text-on-secondary-container dark:text-secondary-fixed rounded-lg text-sm font-bold">
                            {t2.dep_tm ? t2.dep_tm.slice(0,5) : t2.tm ?? '?'}
                          </span>
                        ))}
                        {times.length > 12 && <span className="px-3 py-1.5 text-outline dark:text-slate-400 text-sm">+{times.length-12}개</span>}
                      </div>
                    </div>
                  )}
                </div>
              )
            })()}
          </div>

          {/* 도서관 좌석 — API 연동 */}
          <div className="card p-6 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-3 mb-4">
                <span className="material-symbols-outlined text-primary dark:text-secondary-fixed p-2 bg-secondary-container/30 dark:bg-secondary-fixed/10 rounded-lg">auto_stories</span>
                <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white">도서관 좌석 현황</h3>
              </div>
              {statsLoading ? (
                <div className="flex justify-center py-6"><div className="w-5 h-5 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
              ) : (
                <>
                  <div className="flex items-baseline gap-2 mb-2">
                    <span className="text-4xl font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk']">{sv('library_available')}</span>
                    <span className="text-on-surface-variant dark:text-slate-400">/ {sv('library_total')}{su('library_total')}</span>
                  </div>
                  <div className="w-full bg-surface-container dark:bg-slate-700 rounded-full h-3 mb-4">
                    <div className="h-full bg-secondary dark:bg-secondary-fixed rounded-full transition-all" style={{ width: `${libPct}%` }} />
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    {[
                      ['library_1f','library_2f'],
                      ['library_3f','library_digital']
                    ].flat().map(key => stats[key] && (
                      <div key={key} className="bg-surface-container-low dark:bg-slate-800 p-2 rounded-lg">
                        <p className="text-label-md text-outline dark:text-slate-400">{stats[key].label}</p>
                        <p className="font-bold text-primary dark:text-white">{stats[key].value}{stats[key].unit}</p>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
            <p className="text-[10px] text-outline dark:text-slate-500 mt-4 text-center">관리자가 수시로 업데이트하는 현황입니다</p>
          </div>

          {/* 기숙사 안내 */}
          <div className="lg:col-span-2 card p-6">
            <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary dark:text-secondary-fixed p-2 bg-secondary-container/30 dark:bg-secondary-fixed/10 rounded-lg text-[20px]">apartment</span>
              기숙사 안내
            </h3>
            {statsLoading ? (
              <div className="flex justify-center py-6"><div className="w-5 h-5 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center gap-3 p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">apartment</span>
                  <div>
                    <p className="text-xs text-outline dark:text-slate-400">건물명</p>
                    <p className="font-bold text-primary dark:text-white">{sv('dorm_building', 'IT관')}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-3 bg-surface-container-low dark:bg-slate-800 rounded-xl">
                  <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">campaign</span>
                  <div>
                    <p className="text-xs text-outline dark:text-slate-400">공지</p>
                    <p className="font-bold text-primary dark:text-white">{sv('dorm_notice', '정상 운영 중')}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-xl text-sm text-blue-700 dark:text-blue-300">
                  <span className="material-symbols-outlined text-[18px]">info</span>
                  실시간 IoT 연동 준비 중입니다. 상세 현황은 기숙사 관리실에 문의하세요.
                </div>
              </div>
            )}
          </div>

          {/* 주요 편의시설 */}
          <div className="card p-6">
            <h3 className="font-bold text-primary dark:text-white mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">map</span>주요 편의시설
            </h3>
            <div className="grid grid-cols-3 gap-3">
              {AMENITIES.map(a => (
                <button key={a.label} className="flex flex-col items-center gap-2 p-3 rounded-xl bg-surface-container-low dark:bg-slate-800 hover:bg-secondary-container/20 dark:hover:bg-secondary-fixed/10 active:scale-95 transition-all">
                  <div className="w-12 h-12 rounded-full bg-white dark:bg-slate-900 flex items-center justify-center shadow-sm border border-slate-100 dark:border-slate-700">
                    <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">{a.icon}</span>
                  </div>
                  <span className="text-[11px] font-medium text-on-surface dark:text-slate-300">{a.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 빠른 현황 카드 — API 연동 */}
          <div className="lg:col-span-3 grid grid-cols-2 lg:grid-cols-4 gap-4">
            {statsLoading ? (
              <div className="col-span-4 flex justify-center py-6"><div className="w-6 h-6 border-4 border-primary/20 border-t-primary rounded-full animate-spin" /></div>
            ) : [
              { key: 'studyroom_available', icon: 'meeting_room',  label: '스터디룸 예약 가능', color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
              { key: null,                  icon: 'construction',  label: '시설 수리 요청',      sub: '빠른 접수 가능', color: 'bg-secondary-container dark:bg-secondary-fixed/10 border border-secondary-fixed/30' },
              { key: 'locker_arrived',      icon: 'package_2',     label: '택배 보관함 도착',   color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
              { key: 'parking_available',   icon: 'local_parking', label: '주차장 잔여',         color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
            ].map((c, i) => (
              <button key={i} className={`p-5 rounded-2xl ${c.color} flex flex-col items-center justify-center gap-2 active:scale-95 transition-transform shadow-sm`}>
                <span className="material-symbols-outlined text-3xl text-primary dark:text-secondary-fixed">{c.icon}</span>
                <span className="font-bold text-sm text-primary dark:text-white">{c.label}</span>
                <span className="text-label-md text-outline dark:text-slate-400">
                  {c.key ? `${sv(c.key)}${su(c.key)}` : c.sub}
                </span>
              </button>
            ))}
          </div>

        </div>
      )}
    </Layout>
  )
}
