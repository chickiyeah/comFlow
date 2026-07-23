// 전역 네비게이션 구조 (대메뉴 → 중메뉴 → 소메뉴). SideNav(데스크탑 rail+flyout)와
// TopNav 모바일 드로어가 이 하나의 설정을 공유해 두 화면의 메뉴 구성이 항상 일치한다.
// item: { key, icon, path? , children? }  — path가 있으면 리프(클릭 시 이동), children이 있으면 확장 가능한 그룹.
export const NAV_CATEGORIES = [
  { key: 'dashboard', icon: 'dashboard', path: '/dashboard' },
  {
    key: 'academicGroup', icon: 'school',
    children: [
      { key: 'academic', icon: 'school', path: '/academic' },
      { key: 'calendar', icon: 'calendar_month', path: '/calendar' },
      { key: 'notices', icon: 'campaign', path: '/notices' },
    ],
  },
  {
    key: 'classroomGroup', icon: 'cast_for_education',
    children: [
      { key: 'classroom', icon: 'cast_for_education', path: '/classroom' },
      {
        key: 'kmateGroup', icon: 'smart_toy',
        children: [
          { key: 'kmate', icon: 'forum', path: '/kmate' },
          { key: 'exam', icon: 'timer', path: '/exam' },
        ],
      },
    ],
  },
  {
    key: 'careerGroup', icon: 'work',
    children: [
      { key: 'career', icon: 'work', path: '/career' },
      { key: 'interview', icon: 'record_voice_over', path: '/interview' },
    ],
  },
  {
    key: 'studyGroup', icon: 'menu_book',
    children: [
      { key: 'courses', icon: 'smart_display', path: '/courses' },
      { key: 'quizzes', icon: 'quiz', path: '/quizzes' },
      { key: 'study', icon: 'groups', path: '/study' },
      { key: 'technical', icon: 'description', path: '/technical' },
    ],
  },
  { key: 'facilities', icon: 'corporate_fare', path: '/facilities' },
  { key: 'profile', icon: 'manage_accounts', path: '/profile' },
]

/** 역할 기반 추가 항목(관리자/교수). role: 'ROLE_ADMIN' | 'ROLE_PROFESSOR' | 'ROLE_STUDENT' */
export function roleGatedItems(role) {
  const items = []
  if (role === 'ROLE_PROFESSOR' || role === 'ROLE_ADMIN') {
    items.push({ key: 'professor', icon: 'cast_for_education', path: '/professor' })
  }
  if (role === 'ROLE_ADMIN') {
    items.push({ key: 'admin', icon: 'admin_panel_settings', path: '/admin' })
  }
  return items
}

/** 어떤 노드(및 자손) 안에 이 경로가 있는지 재귀 판정 — 대/중메뉴 active 하이라이트용. */
export function containsPath(item, pathname) {
  if (item.path) {
    return pathname === item.path || pathname.startsWith(item.path + '/')
  }
  if (item.children) {
    return item.children.some(child => containsPath(child, pathname))
  }
  return false
}

/** 모든 리프 노드를 평탄화 — 검색/사이트맵 등에 사용. */
export function flattenLeaves(items = NAV_CATEGORIES) {
  return items.flatMap(item => item.children ? flattenLeaves(item.children) : [item])
}
