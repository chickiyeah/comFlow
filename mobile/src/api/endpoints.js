import api, { unwrap } from './client';

// 인증
export async function login(username, password) {
  const res = await api.post('/auth/login', { username, password });
  return unwrap(res); // { accessToken, ... }
}

// 성적
export async function getMyGrades() {
  const res = await api.get('/grades/me');
  return unwrap(res); // { gpa, totalCredits, grades[] }
}

// 출결
export async function getMyAttendance() {
  const res = await api.get('/attendance/me');
  return unwrap(res); // { attendanceRate, ... }
}

// 오늘 강의
export async function getTodaySchedule() {
  const res = await api.get('/schedule/me/today');
  return unwrap(res); // [ { ... } ]
}

// AI 챗 (학과 자료 RAG, 인증 불필요)
export async function komjeongChat(message) {
  const res = await api.post('/komjeong/chat', { message });
  return unwrap(res); // { answer }
}

// 진로 — 취업 준비 활동
export async function getCareerActivities() {
  const res = await api.get('/career/activities');
  return unwrap(res); // [ { ... } ]
}

// 프로필
export async function getMyProfile() {
  const res = await api.get('/profile/me');
  return unwrap(res); // { name, studentId, department, grade, semester }
}
