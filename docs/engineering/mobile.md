# 엔지니어링 — Mobile (React Native)

> 실행/빌드/디버그/테스트/릴리스 가이드. **검증된 절차만** 적는다.
> 빠른 실행은 [`mobile/README.md`](../../mobile/README.md), 이 문서는 심화·플랫폼별 디테일.

## 현재 상태
- 인증 코드(PKCE + Bearer) 완료. **Android · iOS 로그인 E2E 모두 검증 완료(2026-06-25).**
- iOS: iPhone 17 시뮬레이터(iOS 26, Xcode 26.5)에서 로그인 → `/api/me` 200
  (tester/Test User/tester@toss.local/roles=USER) 확인.

## iOS 사전 준비 (검증 완료 2026-06-25)
1. **xcode-select 전환** (Xcode.app 설치 후 1회, sudo):
   ```bash
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   sudo xcodebuild -license accept
   ```
2. **CocoaPods**: `brew install cocoapods` → `cd ios && pod install` → `TossMobile.xcworkspace` 생성.
   이후 Xcode는 `.xcodeproj` 가 아니라 **`.xcworkspace`** 를 연다.
   (주의: brew cocoapods 는 ruby 4.x 를 끌고 와 기본 `ruby` 를 바꿈 — 싫으면 `brew unlink ruby`, `pod` 영향 없음.)
3. **app-auth 네이티브 배선** (커밋 b0e6c5f):
   - `Info.plist` `CFBundleURLTypes` 에 `com.toss.app` scheme
   - `AppDelegate.swift`: `RNAppAuthAuthorizationFlowManager` 준수 + `authorizationFlowManagerDelegate`
     프로퍼티 + `application(_:open:options:)` 핸들러
   - 브리징 헤더 `TossMobile/TossMobile-Bridging-Header.h` (RNAppAuthAuthorizationFlowManager.h import)
     + pbxproj `SWIFT_OBJC_BRIDGING_HEADER`(Debug/Release)
4. **Keychain entitlement** (커밋 1a48b1c) — ⚠️ 없으면 react-native-keychain 이
   `errSecMissingEntitlement(-34018)` "Internal error when a required entitlement isn't present." 로
   토큰 저장/조회 실패:
   - `TossMobile/TossMobile.entitlements`: `keychain-access-groups = $(AppIdentifierPrefix)$(CFBundleIdentifier)`
   - pbxproj `CODE_SIGN_ENTITLEMENTS`(Debug/Release). 시뮬레이터 빌드는 ad-hoc 서명(`CODE_SIGN_IDENTITY="-"`)으로 적용.
5. **시뮬레이터 HTTPS 신뢰** (mkcert 로컬 CA, 세션마다):
   ```bash
   xcrun simctl keychain booted add-root-cert "$(mkcert -CAROOT)/rootCA.pem"
   ```
   > iOS 시뮬레이터는 host의 localhost에 직접 도달 → Android의 `adb reverse` 불필요.
   > ATS 는 Info.plist `NSAllowsLocalNetworking=true` 로 localhost 허용(이미 설정됨).

## iOS 실행 (로그인 E2E)
```bash
# 1) 스택: Keycloak + backend + Postgres
docker compose -f infra/docker-compose.yml up -d           # Keycloak :8443
docker compose -f backend/compose.yaml up -d               # app Postgres
cd backend && JAVA_HOME=~/.sdkman/candidates/java/25.0.3-amzn ./gradlew bootRun   # :8080
# 2) 시뮬레이터 + Metro + 앱
xcrun simctl boot "iPhone 17"; open -a Simulator
cd mobile && npm start                                     # Metro :8081 (별도 터미널)
npm run ios                                                # 빌드·설치·실행
# 3) mkcert CA 신뢰(위 5번) 후 앱에서 "Keycloak 로그인" → tester / test1234
```
검증됨: 로그인 → `/api/me` 200 (tester, roles=USER).

## Android
[`mobile/README.md`](../../mobile/README.md) 의 "로컬 실행(Android)" 절에 검증된 절차 있음
(에뮬레이터·Metro·adb reverse·mkcert 사용자 저장소 주입 등).

## 테스트
- 현재: jest 인증 화면 렌더 테스트.
- TODO: 컴포넌트 테스트 확대, **Detox E2E**(로그인 플로우).

## TODO
- [ ] iOS 빌드 성공 후 위 절차를 "검증됨"으로 확정하고 mobile/README iOS 절 보강
- [ ] 차트 라이브러리 선정 반영(Victory Native / `@wuba/react-native-echarts`)
