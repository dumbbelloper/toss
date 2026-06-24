# mobile

React Native (bare CLI) 통합 모바일 앱 — iOS·Android 한 코드베이스.

## 인증 (네이티브 PKCE)

웹(BFF)과 달리 모바일은 **public client + Authorization Code + PKCE**:

- `react-native-app-auth` — Keycloak `toss-mobile` client 로 PKCE 로그인
- `react-native-keychain` — access/refresh token 을 Keychain(iOS)/Keystore(Android)에 보관
- 백엔드 REST API 는 **Bearer JWT** 로 호출 (웹의 세션 쿠키와 다른 갈래 — 백엔드에 resource server 인증 추가 예정)
- redirect scheme: `com.toss.app://oauth2redirect` (iOS·Android 공통)

## 차트

| 역할 | 라이브러리 |
|------|-----------|
| 수학(scale/shape) | `d3-scale` · `d3-shape` (web SPA 와 공유) |
| 렌더링 | `react-native-svg` / Skia |
| 커스텀 조합형 | **Victory Native (XL)** — Skia 기반, 제스처·고성능 |
| 헤비/완성형 | **`@wuba/react-native-echarts`** (ECharts) |

> 스캐폴드 예정 (Task #5). bare CLI 라 iOS(CocoaPods/Xcode)·Android(SDK) 네이티브 툴체인 필요.
