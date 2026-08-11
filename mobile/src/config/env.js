import Constants from 'expo-constants';

// API 베이스 URL — 변경하려면 app.json 의 expo.extra.apiBaseUrl 을 수정하거나
// 아래 기본값을 바꾸세요. nginx 가 /api -> 백엔드 8080 으로 프록시합니다.
export const API_BASE_URL =
  Constants.expoConfig?.extra?.apiBaseUrl ||
  Constants.manifest?.extra?.apiBaseUrl ||
  'https://campusflow.jvision.org/api';

export const TOKEN_KEY = 'campusflow-token';
