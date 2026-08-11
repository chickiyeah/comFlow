// 현재 학기 계산 — 브라우저(클라이언트) 현재 날짜 기준
// 3~8월 = 1학기, 9~12월 = 2학기, 1~2월 = 직전 학년도 2학기
export function getCurrentTerm(date = new Date()) {
  const month = date.getMonth() + 1
  const y = date.getFullYear()
  let semester, year
  if (month >= 3 && month <= 8) { semester = 1; year = y }
  else if (month >= 9) { semester = 2; year = y }
  else { semester = 2; year = y - 1 } // 1~2월은 직전 학년도 2학기
  return { year, semester, smrCode: semester === 1 ? 'SU002001' : 'SU002002' }
}

// 포털 시간표 연도 드롭다운 옵션 (현재 학년도부터 과거로 N개)
export function recentYears(count = 4, date = new Date()) {
  const base = getCurrentTerm(date).year
  return Array.from({ length: count }, (_, i) => String(base - i))
}
