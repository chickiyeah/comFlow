import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import Layout from '../components/layout/Layout'
import { searchBooks } from '../api/library'

const AMENITIES = [
  { icon: 'fitness_center', label: '피트니스'  },
  { icon: 'restaurant',     label: '학식 식당' },
  { icon: 'local_cafe',     label: '카페테리아'},
  { icon: 'print',          label: '복사실'    },
  { icon: 'local_library',  label: '도서관'    },
  { icon: 'sports_soccer',  label: '체육관'    },
]

const SHUTTLE = [
  { route: 'A노선 (공대 ↔ 중앙도서관)', eta: '3분 전',  active: true  },
  { route: 'B노선 (기숙사 행)',          eta: '10분 후', active: false },
  { route: 'C노선 (역전 셔틀)',          eta: '15분 후', active: false },
]

const BOOK_CATEGORIES = ['IT/컴퓨터', '수학/통계', '영어', '취업/자격증', '교양', '소설']

export default function Facilities() {
  const { t } = useTranslation()
  const [facTab, setFacTab] = useState('campus')
  const [bookKeyword, setBookKeyword] = useState('')
  const [bookCategory, setBookCategory] = useState('')
  const [books, setBooks] = useState([])
  const [bookLoading, setBookLoading] = useState(false)
  const [searched, setSearched] = useState(false)

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

  const FAC_TABS = [
    { key: 'campus', label: t('facilities.tab_shuttle') + ' · ' + t('facilities.tab_dorm') },
    { key: 'books',  label: t('facilities.tab_books') },
  ]
  return (
    <Layout title={t('facilities.title')}>
      <div className="mb-6">
        <h2 className="font-['Space_Grotesk'] text-2xl font-bold text-primary dark:text-white">{t('facilities.title')}</h2>
        <p className="text-on-surface-variant dark:text-slate-400 text-sm mt-1">{t('facilities.subtitle')}</p>
      </div>

      {/* Tab switcher */}
      <div className="flex gap-1 p-1 bg-surface-container dark:bg-slate-800 rounded-2xl w-fit mb-6">
        {FAC_TABS.map(t2 => (
          <button key={t2.key} onClick={() => setFacTab(t2.key)}
            className={`px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
              facTab === t2.key ? 'bg-white dark:bg-slate-900 text-primary dark:text-white shadow-sm' : 'text-on-surface-variant dark:text-slate-400 hover:text-primary dark:hover:text-white'
            }`}>{t2.label}</button>
        ))}
      </div>

      {/* Book search tab */}
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

      {facTab === 'campus' && <div className="grid grid-cols-1 lg:grid-cols-3 gap-card_gap">

        {/* Shuttle */}
        <div className="lg:col-span-2 card p-6">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-secondary-container dark:bg-secondary-fixed/20 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary dark:text-secondary-fixed text-[20px]">directions_bus</span>
              </div>
              셔틀버스 안내
            </h3>
            <span className="px-2 py-1 bg-secondary-container dark:bg-secondary-fixed/20 text-on-secondary-container dark:text-secondary-fixed text-[10px] font-bold rounded-full">실시간</span>
          </div>

          <div className="bg-surface-container-low dark:bg-slate-800 rounded-xl p-4 mb-4">
            <div className="flex justify-between items-center mb-3">
              <div>
                <p className="text-label-md text-outline dark:text-slate-400">현재 운행 노선</p>
                <p className="font-bold text-primary dark:text-white">A노선: 공대 ↔ 중앙도서관</p>
              </div>
              <div className="text-right">
                <p className="text-label-md text-outline dark:text-slate-400">도착 예정</p>
                <p className="font-black text-error">3분 전</p>
              </div>
            </div>
            <div className="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-full">
              <div className="h-full bg-primary dark:bg-primary-container rounded-full w-[70%] transition-all" />
            </div>
          </div>

          <div className="space-y-2">
            {SHUTTLE.map((s, i) => (
              <div key={i} className="flex items-center justify-between p-3 border-b border-slate-100 dark:border-slate-800 last:border-0">
                <span className={`text-sm font-medium ${s.active ? 'text-primary dark:text-secondary-fixed' : 'text-on-surface dark:text-slate-300'}`}>{s.route}</span>
                <span className={`text-sm ${s.active ? 'text-error font-bold' : 'text-outline dark:text-slate-400'}`}>{s.eta}</span>
              </div>
            ))}
          </div>

          <button className="w-full mt-4 py-3 bg-primary dark:bg-primary-container text-white rounded-xl font-bold text-sm flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-95 transition-transform">
            <span className="material-symbols-outlined text-[18px]">edit_calendar</span>정기권 신청하기
          </button>
        </div>

        {/* Library seats */}
        <div className="card p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 mb-4">
              <span className="material-symbols-outlined text-primary dark:text-secondary-fixed p-2 bg-secondary-container/30 dark:bg-secondary-fixed/10 rounded-lg">auto_stories</span>
              <h3 className="font-['Space_Grotesk'] text-lg font-semibold text-primary dark:text-white">도서관 실시간 좌석</h3>
            </div>
            <div className="flex items-baseline gap-2 mb-2">
              <span className="text-4xl font-black text-primary dark:text-secondary-fixed font-['Space_Grotesk']">45</span>
              <span className="text-on-surface-variant dark:text-slate-400">/ 120석</span>
            </div>
            <div className="w-full bg-surface-container dark:bg-slate-700 rounded-full h-3 mb-4">
              <div className="h-full bg-secondary dark:bg-secondary-fixed rounded-full" style={{ width: '37.5%' }} />
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm">
              {[['1층 열람실', '12석'], ['2층 스터디룸', '8석'], ['3층 그룹실', '15석'], ['디지털 열람실', '10석']].map(([n, v]) => (
                <div key={n} className="bg-surface-container-low dark:bg-slate-800 p-2 rounded-lg">
                  <p className="text-label-md text-outline dark:text-slate-400">{n}</p>
                  <p className="font-bold text-primary dark:text-white">{v}</p>
                </div>
              ))}
            </div>
          </div>
          <button className="w-full mt-4 py-2.5 border border-outline dark:border-slate-700 text-on-surface-variant dark:text-slate-300 rounded-xl text-label-md hover:bg-surface-container dark:hover:bg-slate-800 transition-colors">
            좌석 예약하기
          </button>
        </div>

        {/* Dormitory */}
        <div className="lg:col-span-2 card overflow-hidden">
          <div className="relative h-36">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuAtWWxBVdCgiNNAYClnuqLNlkpEuYfNwSrmGuDF5iu-6vmy6Tl05wcn7tlHcHJFdEK8hwkr9D3M8FLcs-rg8gX8eyRWY1Lpf-zfjDuXNh6esyMFo6vclnsAJJQDgmjSHYngh0YFY-KbU3vPJpFOxi3XsltUDIutW3_ky-7cs5DB2DwPZr5sl3TxLizX6d7WJ7kMz-S3-hJYpeoHa2zWYYrfDxtVOlV5nikPLqtQmKr5-8ab9sJLC2biMejYyA83SXbM16v1Zg91qK8"
              alt="기숙사" className="w-full h-full object-cover dark:opacity-60"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end p-4">
              <div>
                <h3 className="text-white font-['Space_Grotesk'] text-lg font-semibold">IT관 기숙사</h3>
                <p className="text-secondary-fixed text-sm">302호 · 거주 중</p>
              </div>
            </div>
          </div>
          <div className="p-5 grid grid-cols-3 gap-3">
            {[
              { icon: 'thermostat', label: '실내 온도', val: '24.5°C' },
              { icon: 'lightbulb',  label: '조명 상태', val: '자동'  },
              { icon: 'encrypted', label: '보안 등급',  val: 'S+'    },
            ].map(s => (
              <div key={s.label} className="p-3 rounded-xl bg-surface-container-low dark:bg-slate-800 text-center">
                <span className="material-symbols-outlined text-primary dark:text-secondary-fixed">{s.icon}</span>
                <p className="text-[10px] text-outline dark:text-slate-400 mt-1">{s.label}</p>
                <p className="font-bold text-primary dark:text-white">{s.val}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Amenities */}
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

        {/* Study room + repair */}
        <div className="lg:col-span-3 grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { icon: 'meeting_room',   label: '스터디룸 예약',  sub: '예약 가능: 12개', color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
            { icon: 'construction',   label: '시설 수리 요청', sub: '빠른 접수 가능',   color: 'bg-secondary-container dark:bg-secondary-fixed/10 border border-secondary-fixed/30' },
            { icon: 'package_2',      label: '택배 보관함',    sub: '현재 3개 도착',    color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
            { icon: 'local_parking',  label: '주차 현황',      sub: '잔여 42대',        color: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800' },
          ].map(c => (
            <button key={c.label} className={`p-5 rounded-2xl ${c.color} flex flex-col items-center justify-center gap-2 active:scale-95 transition-transform shadow-sm`}>
              <span className="material-symbols-outlined text-3xl text-primary dark:text-secondary-fixed">{c.icon}</span>
              <span className="font-bold text-sm text-primary dark:text-white">{c.label}</span>
              <span className="text-label-md text-outline dark:text-slate-400">{c.sub}</span>
            </button>
          ))}
        </div>
      </div>}
    </Layout>
  )
}
