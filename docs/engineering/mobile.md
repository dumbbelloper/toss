# 엔지니어링 — Mobile (React Native)

> 실행/빌드/디버그/테스트/릴리스 가이드. **검증된 절차만** 적는다.
> 빠른 실행은 [`mobile/README.md`](../../mobile/README.md), 이 문서는 심화·플랫폼별 디테일.

## 현재 상태
- 인증 코드(PKCE + Bearer) 완료. **Android 로그인 E2E 검증 완료.**
- **iOS: 진행 중** — CocoaPods 설치 완료, Info.plist URL scheme 추가 완료.
  남은 것: `pod install` → AppDelegate app-auth 배선 → 시뮬레이터 mkcert 신뢰 → 빌드·로그인 E2E.

## iOS 사전 준비 (검증되면 확정)
1. **xcode-select 전환** (Xcode.app 설치 후 1회, sudo):
   ```bash
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   sudo xcodebuild -license accept
   ```
2. **CocoaPods**: `brew install cocoapods` (완료) → `cd ios && pod install` → `TossMobile.xcworkspace` 생성.
   이후 Xcode는 `.xcodeproj` 가 아니라 **`.xcworkspace`** 를 연다.
3. **app-auth 네이티브 배선**:
   - `Info.plist` `CFBundleURLTypes` 에 `com.toss.app` scheme (완료)
   - `AppDelegate.swift` 에 `RNAppAuthAuthorizationFlowManager` 준수 + `open url` 핸들러 (pod 설치 후 작업)
4. **시뮬레이터 HTTPS 신뢰** (mkcert 로컬 CA):
   ```bash
   xcrun simctl keychain booted add-root-cert "$(mkcert -CAROOT)/rootCA.pem"
   ```
   > iOS 시뮬레이터는 host의 localhost에 직접 도달 → Android의 `adb reverse` 불필요.

## Android
[`mobile/README.md`](../../mobile/README.md) 의 "로컬 실행(Android)" 절에 검증된 절차 있음
(에뮬레이터·Metro·adb reverse·mkcert 사용자 저장소 주입 등).

## 테스트
- 현재: jest 인증 화면 렌더 테스트.
- TODO: 컴포넌트 테스트 확대, **Detox E2E**(로그인 플로우).

## TODO
- [ ] iOS 빌드 성공 후 위 절차를 "검증됨"으로 확정하고 mobile/README iOS 절 보강
- [ ] 차트 라이브러리 선정 반영(Victory Native / `@wuba/react-native-echarts`)
