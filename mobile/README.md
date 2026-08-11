# CampusFlow Mobile

CampusFlow 모바일 앱 — Expo (React Native, JavaScript).
기존 웹(`../frontend`)과 동일한 Spring Boot 백엔드 API를 그대로 사용합니다.

- 백엔드 API: `https://campusflow.jvision.org/api` (nginx 가 `/api` → 백엔드 `:8080` 프록시, 공개 도메인이라 휴대폰에서 접근 가능)
- 디자인 톤: 딥 네이비 `#00236f` + 라임 포인트 `#bff365`, 라운드 카드 (웹과 동일)

## 실행 방법

```bash
cd C:\Users\0_8_2\comFlow\mobile
npm install          # 의존성 설치 (최초 1회)
npx expo start       # 개발 서버 + QR 코드 출력
```

1. 휴대폰에 **Expo Go** 앱 설치 (App Store / Google Play).
2. `npx expo start` 실행 후 터미널/브라우저에 표시되는 **QR 코드를 Expo Go 로 스캔**.
   - iOS: 카메라 앱으로 스캔 → Expo Go 로 열기
   - Android: Expo Go 앱 내 "Scan QR code"
3. 휴대폰과 PC 가 **같은 Wi-Fi** 가 아니어도, API 는 공개 도메인이라 동작합니다.
   (Expo 번들 자체는 같은 네트워크가 편하지만, 안 되면 `npx expo start --tunnel` 사용)

### 시뮬레이터 / 에뮬레이터

```bash
npx expo start --ios       # iOS 시뮬레이터 (macOS)
npx expo start --android   # Android 에뮬레이터
```

## API 베이스 URL 변경법

베이스 URL 은 두 곳에서 관리됩니다.

1. **`app.json`** 의 `expo.extra.apiBaseUrl` — 우선 적용됨.
   ```json
   "extra": { "apiBaseUrl": "https://campusflow.jvision.org/api" }
   ```
2. **`src/config/env.js`** 의 기본값 — `extra` 가 없을 때 폴백.

로컬 백엔드(예: PC `:8080`)로 붙으려면 `app.json` 의 값을
`http://<PC-LAN-IP>:8080/api` 로 바꾸세요. (휴대폰에서 `localhost` 는 휴대폰 자신을
가리키므로 LAN IP 를 써야 합니다.)

## 인증 흐름

- 로그인: `POST /api/auth/login` `{ username, password }` → 응답 `data.accessToken`.
- 토큰은 `expo-secure-store` 에 저장 (미지원 환경에서는 `@react-native-async-storage/async-storage` 로 자동 폴백 — `src/lib/storage.js`).
- 이후 모든 요청에 axios 인터셉터(`src/api/client.js`)가 `Authorization: Bearer <token>` 자동 첨부.
- 토큰이 만료되어 `401` 이 오면 자동 로그아웃 → 로그인 화면으로 전환.

## 화면 구성

| 탭 | 화면 | 사용 API |
|----|------|----------|
| (로그인 전) | Login | `POST /auth/login` |
| 대시보드 | Dashboard | `GET /grades/me`, `GET /attendance/me`, `GET /schedule/me/today` |
| AI 챗 | Chat | `POST /komjeong/chat` (인증 불필요, 학과 자료 기반 RAG) |
| 진로 | Career | `GET /career/activities` |
| 프로필 | Profile | `GET /profile/me` + 로그아웃 |

모든 백엔드 응답은 `{ success, data }` 래핑이며, `src/api/client.js` 의 `unwrap()` 이 `data` 를 꺼냅니다.

## 폴더 구조

```
mobile/
├─ App.js                     # 앱 진입점 (Provider + Navigator)
├─ index.js                   # Expo registerRootComponent
├─ app.json                   # Expo 설정 (이름, 색상, apiBaseUrl)
├─ babel.config.js
├─ package.json
├─ assets/                    # 아이콘/스플래시 (README 참고)
└─ src/
   ├─ api/
   │  ├─ client.js            # axios 인스턴스 + JWT 인터셉터 + unwrap()
   │  └─ endpoints.js         # 백엔드 엔드포인트 래퍼 함수
   ├─ config/
   │  └─ env.js               # API_BASE_URL, TOKEN_KEY
   ├─ context/
   │  └─ AuthContext.js       # 로그인 상태/토큰 관리
   ├─ lib/
   │  └─ storage.js           # SecureStore ↔ AsyncStorage 폴백 저장소
   ├─ components/
   │  ├─ Screen.js            # SafeArea 래퍼
   │  ├─ Card.js              # 라운드 카드
   │  ├─ Button.js            # primary/accent/ghost 버튼
   │  └─ StatCard.js          # 대시보드 통계 카드
   ├─ navigation/
   │  └─ RootNavigator.js     # 인증 분기 + 하단 탭
   └─ screens/
      ├─ LoginScreen.js
      ├─ DashboardScreen.js
      ├─ ChatScreen.js
      ├─ CareerScreen.js
      └─ ProfileScreen.js
```

## 참고

- Expo SDK 51 / React Native 0.74 / React 18 기준.
- 백엔드는 변경하지 않습니다. 신규 엔드포인트가 필요하면 웹/백엔드와 동일 규칙(`{ success, data }`)을 따릅니다.
