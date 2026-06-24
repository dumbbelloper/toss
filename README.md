# toss

토스증권 Open API 기반 주식 모니터링·매매 플랫폼. **BFF 아키텍처**로 1개의 백엔드와 2개의 클라이언트(웹 SPA·React Native 모바일)를 운영하며, 사용자 인증·인가는 **Keycloak(OIDC)** 으로 처리한다.

## 아키텍처

```
  ┌─ web (React SPA) ──────────┐
  ├─ mobile (React Native) ────┤──① OIDC 로그인 ──▶ Keycloak (Authorization Server)
  └────────────────────────────┘                       │ 토큰 발급
         │                                              │
         │ 웹: httpOnly 세션쿠키 (BFF)                   │ JWKS
         │ 모바일: Bearer JWT (PKCE)                     ▼
         ▼                                      backend (Spring) ──검증
  backend (Spring)                                      │
         │ ② Toss client_secret (machine-to-machine)
         ▼
  Toss Securities Open API
```

- **레이어 ① 사용자 인증** — Keycloak이 담당. 웹은 BFF(백엔드가 confidential client로 code flow 중개, 브라우저엔 토큰 미노출), 모바일은 public client + PKCE.
- **레이어 ② Toss 연동** — 백엔드만 `client_secret`을 보유하는 machine-to-machine. Keycloak과 무관.

## 디렉터리 구조

| 경로 | 내용 |
|------|------|
| [`backend/`](backend) | Spring Boot (Java 25). Toss 연동 + BFF 인증 + REST API |
| [`web/`](web) | React + Vite + Tailwind 4 + TanStack (SPA) |
| [`mobile/`](mobile) | React Native (bare CLI). iOS·Android 통합, AppAuth PKCE |
| [`infra/`](infra) | Keycloak + Postgres docker-compose, realm export |
| [`docs/`](docs) | Toss API 명세(단일 진실 공급원) 등 공유 문서 |

각 클라이언트는 자체 툴체인(Gradle / npm / CocoaPods·Android SDK)을 폴더 안에서 그대로 사용하는 **느슨한 폴리글랏 모노레포**다.

## 시작하기

각 앱의 실행법은 해당 폴더의 README를 참고한다.

- 백엔드: [`backend/README.md`](backend/README.md)
- 인증 인프라(Keycloak): [`infra/README.md`](infra/README.md)
