# mobile

React Native (bare CLI) 통합 모바일 앱 — iOS·Android 한 코드베이스.

## 인증 (네이티브 PKCE)

웹(BFF 쿠키 세션)과 달리 모바일은 **public client + Authorization Code + PKCE** 로 토큰을
직접 받아 **Bearer JWT** 로 백엔드를 호출한다(RFC 8252). 시스템 브라우저(Custom Tab /
ASWebAuthenticationSession)에서 로그인하므로 WebView 가 아니다.

- `react-native-app-auth` — Keycloak `toss-mobile` client 로 PKCE 로그인 (`src/auth/oidc.ts`)
- `react-native-keychain` — access/refresh token 을 Keychain(iOS)/Keystore(Android)에 보관 (`src/auth/tokens.ts`)
- 백엔드 REST API 는 **Bearer JWT** 로 호출 → 백엔드 resource-server 체인이 검증 (`src/api/client.ts`)
- `@tanstack/react-query` 로 `useMe`/`useLogin`/`useLogout` (웹 `web/src/lib/auth.ts` 미러, `src/auth/auth.ts`)
- redirect scheme: `com.toss.app://oauth2redirect` (Android `appAuthRedirectScheme` / iOS Info.plist)

엔드포인트/이슈어 등 설정은 `src/config.ts` 에 모여 있다.

> **상태:** Android · iOS E2E 검증 완료 — 로그인 → `/api/me` 200(사용자 정보 표시)까지 동작.
> iOS 상세 절차(CocoaPods·app-auth 배선·Keychain entitlement·시뮬레이터 mkcert 신뢰·실행)는
> [`docs/engineering/mobile.md`](../docs/engineering/mobile.md) 참고.

## 로컬 실행 (Android)

### 사전 준비 (1회)

- `~/.zshrc` 에 `ANDROID_HOME=$HOME/Library/Android/sdk` + PATH(platform-tools/emulator/cmdline-tools/latest/bin)
- AVD 1개 (예: `toss_pixel`). `npm install`.
- **Android 빌드는 JDK 17–21** 사용 (RN 0.86 AGP 는 JDK 25 미지원). `gradle-daemon-jvm.properties`
  가 daemon 을 JDK 21 로 고정하므로 보통 추가 설정 불필요.

### 실행

```bash
emulator -avd toss_pixel              # 1. 에뮬레이터
npm start                             # 2. Metro (별도 터미널)
npm run android                       # 3. 빌드·설치 (네이티브 변경 시에만 재실행)
```

JS/TS 수정은 저장하면 Fast Refresh 로 즉시 반영된다(재빌드 불필요).

### 로그인 E2E 를 위한 추가 설정 (에뮬레이터, 세션마다 — 휘발성)

백엔드·Keycloak 은 HTTPS(mkcert 로컬 CA, issuer `https://localhost:8443`)다. 에뮬레이터가
이를 신뢰·도달하게 하려면:

1. **포트 포워딩** — 에뮬레이터 `localhost` 를 호스트로 연결(issuer·인증서 CN 이 `localhost` 라 그대로 일치):
   ```bash
   adb reverse tcp:8080 tcp:8080   # 백엔드
   adb reverse tcp:8443 tcp:8443   # Keycloak
   ```
2. **호스트에서 백엔드(:8080) + Keycloak(:8443) 기동** (각 README/infra 참고).
3. **앱의 HTTPS 신뢰** — `app-auth` 의 discovery/token 호출은 `android/app/src/debug/` 의
   `network_security_config.xml` 가 mkcert rootCA(`res/raw/mkcert_rootca.pem`)를 신뢰하도록 함.
   **커밋돼 있어 debug 빌드면 자동 적용**(별도 작업 불필요).
4. **브라우저(Custom Tab) 신뢰** — 로그인 페이지는 Chrome 이 연다. mkcert CA 를 에뮬레이터
   **사용자 인증서 저장소**에 설치(`adb root` 필요):
   ```bash
   ROOTCA="$(mkcert -CAROOT)/rootCA.pem"
   HASH=$(openssl x509 -inform PEM -subject_hash_old -noout -in "$ROOTCA")
   adb root
   adb shell "mkdir -p /data/misc/user/0/cacerts-added"
   adb push "$ROOTCA" "/data/misc/user/0/cacerts-added/${HASH}.0"
   adb shell "chown system:system /data/misc/user/0/cacerts-added/${HASH}.0; \
              chmod 644 /data/misc/user/0/cacerts-added/${HASH}.0; \
              chcon u:object_r:misc_user_data_file:s0 /data/misc/user/0/cacerts-added/${HASH}.0"
   ```
   (사용자 저장소는 `/data` 라 재부팅에도 유지된다.)
5. **Chrome 최초 실행(온보딩) 스킵** — Custom Tab 이 막히지 않게:
   ```bash
   adb shell 'echo "_ --no-first-run --disable-fre --no-default-browser-check" > /data/local/tmp/chrome-command-line'
   ```

> ⚠️ **하지 말 것:** mkcert CA 를 system 저장소(`/apex/com.android.conscrypt/cacerts`)에
> bind-mount 로 주입하면 Android 14 에뮬레이터에서 **zygote 가 크래시 루프**에 빠져 부팅이
> 멈춘다. 위의 **사용자 저장소(`/data/misc/user`)** 방식을 쓸 것. (system 을 건드렸다면
> `adb reboot` 로 복구.)

## 차트 (TODO)

| 역할 | 라이브러리 |
|------|-----------|
| 수학(scale/shape) | `d3-scale` · `d3-shape` (web SPA 와 공유) |
| 렌더링 | `react-native-svg` / Skia |
| 커스텀 조합형 | **Victory Native (XL)** — Skia 기반, 제스처·고성능 |
| 헤비/완성형 | **`@wuba/react-native-echarts`** (ECharts) |
