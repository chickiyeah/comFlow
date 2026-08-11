# 앱 아이콘 / 스플래시 이미지

`app.json` 은 다음 이미지를 참조합니다. 실제 이미지가 없어도 `npx expo start`(개발)는
동작하지만, 빌드(`eas build`) 전에는 아래 파일을 준비하세요.

| 파일 | 권장 크기 | 용도 |
|------|-----------|------|
| `icon.png` | 1024 x 1024 | 앱 아이콘 |
| `adaptive-icon.png` | 1024 x 1024 | Android 적응형 아이콘 (전경) |
| `splash.png` | 1284 x 2778 | 스플래시 화면 |
| `favicon.png` | 48 x 48 | 웹 파비콘 |

브랜드 색상: 딥 네이비 `#00236f`, 라임 포인트 `#bff365`.

> 개발 중 아이콘 경고가 거슬리면 `app.json` 의 `icon`/`splash`/`adaptiveIcon`/`favicon`
> 항목을 잠시 제거해도 됩니다.
