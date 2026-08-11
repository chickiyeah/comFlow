// 글자수 카운터 — 백엔드 CharCounter(counter.py) 의 프론트 포팅.
// 검토 UI 에서 실시간 글자수(공백포함/제외) 표시 + 적정 여부 판정에 사용.

export const GOOD_MIN_RATIO = 0.85
export const GOOD_MAX_RATIO = 0.98

// 글자수 계산. includeSpaces=false 면 공백·개행 제외
export function countChars(text, includeSpaces = true) {
  if (!text) return 0
  return includeSpaces ? text.length : text.replace(/\s/g, '').length
}

// charLimitType('공백포함'|'공백제외'|null) → includeSpaces 불리언
export function resolveIncludeSpaces(charLimitType) {
  return !charLimitType || !charLimitType.includes('제외')
}

// 공백포함/제외 둘 다 (UI 표시용)
export function bothCounts(text) {
  return { withSpaces: countChars(text, true), withoutSpaces: countChars(text, false) }
}

// 제한 대비 상태 판정: 'ok' | 'over' | 'short' | 'no_limit'
export function checkLength(text, limit, charLimitType) {
  if (!limit) return { status: 'no_limit', count: countChars(text, true) }
  const includeSpaces = resolveIncludeSpaces(charLimitType)
  const count = countChars(text, includeSpaces)
  const targetMin = Math.floor(limit * GOOD_MIN_RATIO)
  const targetMax = Math.floor(limit * GOOD_MAX_RATIO)
  let status
  if (count > limit) status = 'over'
  else if (count < targetMin) status = 'short'
  else status = 'ok'
  return { status, count, limit, includeSpaces, targetMin, targetMax }
}
