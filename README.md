# 곳간 (Gotgan)

**토스증권 Open API 기반 개인 자산 슈퍼앱.** 자산 관리 · 백테스팅 · 세후 배당 시뮬레이터를 하나의 통일된 경험으로 묶는다. 하나의 백엔드(BFF)와 두 클라이언트(웹 SPA · React Native 모바일)를 운영하며, 인증·인가는 **Keycloak(OIDC)** 으로 처리한다.

## 무엇을 하나 (3개의 기둥)

| 기둥 | 내용 |
|------|------|
| **자산 관리** | 토스증권 API로 보유·평가·시세를 실시간 조회. 손익·차트. |
| **백테스팅** | 미국·한국 ETF 수십 년 데이터(내부 DB)로 전략 검증. 배당 재투자·환율·MDD. |
| **배당 시뮬레이터(SIM01)** | 실제 분배금 기반 세후 현금흐름. 국내 vs 해외 ETF 세제 비교, 건보료·종합과세 경고. |

> 한국 ETF 세제(3분류 과세·금융소득종합과세·건강보험료) 규칙은 [`docs/product/korea-tax.md`](docs/product/korea-tax.md)에 단일 출처로 정리(교차검증 완료). 백테스트·시뮬은 **라이브 호출 없이 내부 DB만** 사용하고, 데이터는 매일 08:00 KST 배치로 적재한다.

## 아키텍처

```
  ┌─ web (React SPA) ──────────┐
  ├─ mobile (React Native) ────┤──① OIDC 로그인 ──▶ Keycloak (Authorization Server)
  └────────────────────────────┘                       │ 토큰 발급 · JWKS
         │                                              ▼
         │ 웹: httpOnly 세션쿠키 (BFF)          backend (Spring) ──검증
         │ 모바일: Bearer JWT (PKCE)                    │
         ▼                                              │ ② Toss client_secret (M2M)
  backend (Spring Boot)                                 ▼
                                              Toss Securities Open API
```

- **레이어 ① 사용자 인증** — Keycloak. 웹은 **BFF**(백엔드가 confidential client로 code flow 중개, 브라우저엔 토큰 미노출), 모바일은 **public client + PKCE**. 로그아웃은 RP-initiated end-session(SSO 세션까지 종료).
- **레이어 ② Toss 연동** — 백엔드만 `client_secret`을 보유하는 machine-to-machine.

## 디렉터리 구조

| 경로 | 내용 | 툴체인 |
|------|------|--------|
| [`backend/`](backend) | Spring Boot 4.1 / Java 25. Toss 연동 + BFF 인증 + REST API + 데이터 적재·백테스트·시뮬 엔진 | Gradle |
| [`web/`](web) | React 19 + Vite + Tailwind 4 + TanStack Router/Query (SPA) | pnpm |
| [`mobile/`](mobile) | React Native 0.86 (bare CLI). iOS·Android 통합, react-native-app-auth(PKCE) | npm + CocoaPods + Gradle |
| [`infra/`](infra) | Keycloak 26.5 + Postgres docker-compose, realm export, 곳간 로그인 테마 | Docker |
| [`docs/`](docs) | 제품·디자인·데이터·세제·엔지니어링 문서 | — |

각 앱이 자체 툴체인을 폴더 안에서 쓰는 **느슨한 폴리글랏 모노레포**다.

---

# macOS 개발 환경 셋업

> 검증된 절차(Apple Silicon/Intel 공통). 한 번만 하는 설치와 매번 하는 기동을 구분한다.

## 0. 사전 도구 (Homebrew)

```bash
# 공통
brew install --cask docker          # Docker Desktop (인프라 + 앱 DB)
brew install mkcert nss             # 로컬 HTTPS CA (nss = Firefox 신뢰용)
brew install node                   # Node ≥ 22.11 (web · mobile). nvm 으로 관리해도 됨
corepack enable                     # pnpm 활성화 (web)
brew install --cask temurin@21      # Gradle 실행/Android 빌드용 JDK (백엔드 빌드는 툴체인이 Java 25 자동 다운로드)
                                    #   대안: sdkman → `sdk install java 21-tem`

# 모바일 iOS
xcode-select --install              # 또는 App Store 에서 Xcode 설치
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
sudo xcodebuild -license accept
brew install cocoapods

# 모바일 Android — Android Studio 설치 후 SDK·에뮬레이터 구성, 환경변수:
#   export ANDROID_HOME=$HOME/Library/Android/sdk
#   export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

## 1. 인증 인프라 (Keycloak) — 한 번만 + 기동

```bash
mkcert -install                        # 로컬 CA를 시스템 신뢰 저장소에 등록(관리자 비번)
./infra/keycloak/trust-jvm.sh          # 백엔드 JVM이 mkcert CA를 신뢰하도록 dev-truststore.p12 생성
docker compose -f infra/docker-compose.yml up -d   # Keycloak(:8443) + 전용 Postgres
```

- 콘솔 `https://localhost:8443` · **admin / admin** · realm `toss` · issuer `…/realms/toss`
- **Keycloak 초기화(개발):** `docker compose -f infra/docker-compose.yml down -v && up -d`
  → realm을 JSON에서 깨끗이 재임포트(admin/admin 복귀). **앱 데이터는 별도 DB라 안전.**

## 2. 백엔드 (:8080)

```bash
docker compose -f backend/compose.yaml up -d   # 앱 PostgreSQL(:5432, toss/toss/toss)
cd backend && ./gradlew bootRun                # bootRun 이 dev-truststore.p12 자동 주입
```

- Toss 자격증명은 `backend/src/main/resources/application-local.yml`(gitignore)에 둔다 — 자산 탭(실계좌)에만 필요. 백테스트·시뮬은 없어도 동작.
- 최초 기동 시 Flyway가 스키마 + 유니버스(17 US + 4 KR ETF)를 만든다. 시세·분배금 적재:
  `POST /api/history/backfill` (인증 필요) 또는 매일 08:00 KST 스케줄러.

## 3. 웹 SPA (:5173)

```bash
cd web && pnpm install && pnpm dev
```

`http://localhost:5173` 접속 → 랜딩 → 로그인(`tester` / `test1234`).

## 4. 모바일 (React Native)

```bash
cd mobile && npm install
cd ios && pod install && cd ..

# iOS 시뮬레이터
npm run ios
# 시뮬레이터가 https://localhost:8443(mkcert)을 신뢰하도록 rootCA 등록:
xcrun simctl keychain booted add-root-cert "$(mkcert -CAROOT)/rootCA.pem"

# Android 에뮬레이터 (JDK 21)
cp "$(mkcert -CAROOT)/rootCA.pem" mobile/android/app/src/debug/res/raw/mkcert_rootca.pem  # 한 번만
JAVA_HOME=$(/usr/libexec/java_home -v 21) npm run android
adb reverse tcp:8080 tcp:8080 && adb reverse tcp:8443 tcp:8443 && adb reverse tcp:8081 tcp:8081
```

> Metro(:8081)는 `npm start`로 별도 기동하거나 run 명령이 자동 띄운다.

## 포트 한눈에

| 포트 | 용도 |
|------|------|
| 8080 | backend (BFF + API) |
| 5173 | web (Vite dev) |
| 8081 | Metro (RN 번들러) |
| 8443 / 9000 | Keycloak (HTTPS / 관리) |
| 5432 | 앱 PostgreSQL |

## 개발 자격증명 (로컬 전용)

| 대상 | 값 |
|------|-----|
| Keycloak 콘솔 (master) | `admin` / `admin` |
| 앱 로그인 (toss realm) | `tester` / `test1234` |
| 앱 DB | `toss` / `toss` (db `toss`) |

> 개발 환경엔 OTP를 쓰지 않는다(운영에서만). admin은 master realm 사용자라 **앱 로그인엔 못 쓴다**(앱은 tester).

## 자주 막히는 곳

- **Keycloak HTTPS 경고/JVM 실패** → `mkcert -install` + `trust-jvm.sh` 누락. 둘 다 실행했는지 확인.
- **Android에서 시세/로그인 안 됨** → `adb reverse`(8080·8443·8081) 미설정, 또는 rootCA를 `res/raw`에 미복사.
- **iOS 시뮬레이터 TLS 실패** → `xcrun simctl keychain booted add-root-cert …` 누락.
- **Flyway 새 SQL 미적용** → `./gradlew classes`(리소스 복사) 후 DevTools 재시작.
- **Android Fast Refresh 누락** → 앱 재시작(`adb shell am force-stop com.tossmobile` 후 재실행).

---

## 더 보기

- **제품 비전·로드맵**: [`docs/PRODUCT.md`](docs/PRODUCT.md)
- **한국 ETF 세제 모델**: [`docs/product/korea-tax.md`](docs/product/korea-tax.md)
- **엔지니어링 노트**: [`docs/engineering/`](docs/engineering) (backend · web · mobile)
- **알려진 이슈**: [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md)
- **데이터 출처**: [`docs/data/sources.md`](docs/data/sources.md)
- **문서 체계 전체**: [`docs/README.md`](docs/README.md)
