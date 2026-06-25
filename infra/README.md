# infra

로컬 개발용 인증 인프라 — **Keycloak(OIDC IdP) + 전용 Postgres**.

## 구성

| 파일 | 내용 |
|------|------|
| `docker-compose.yml` | Keycloak 26.5 (`start` 모드, HTTPS) + Keycloak 전용 Postgres 17 |
| `keycloak/import/toss-realm.json` | realm·client·역할·테스트 사용자 (버전관리되는 단일 진실 공급원) |
| `keycloak/tls/*.pem` | mkcert 로컬 인증서 (**gitignore** — 기기 전용) |

> 앱 DB(`backend/compose.yaml`의 postgres)와 Keycloak DB는 분리돼 있다.

## 사전 준비: mkcert 로컬 CA 신뢰

브라우저가 `https://localhost:8443`를 경고 없이 신뢰하려면 한 번만:

```bash
mkcert -install      # 시스템 신뢰 저장소에 로컬 CA 등록 (관리자 비밀번호 필요)
```

인증서 자체는 `keycloak/tls/`에 이미 발급돼 있다. 재발급이 필요하면:

```bash
mkcert -cert-file infra/keycloak/tls/localhost.pem \
       -key-file  infra/keycloak/tls/localhost-key.pem \
       localhost 127.0.0.1 ::1
```

> **백엔드(BFF)의 JVM 신뢰**는 별도다. Spring이 Keycloak 토큰 엔드포인트를 HTTPS로
> 직접 호출하므로 JVM 트러스트스토어도 이 CA를 신뢰해야 한다(Task 3에서 처리).

## 기동 / 정지

```bash
docker compose -f infra/docker-compose.yml up -d      # 기동
docker compose -f infra/docker-compose.yml logs -f keycloak
docker compose -f infra/docker-compose.yml down       # 정지 (-v 로 볼륨까지 삭제)
```

- **관리 콘솔**: https://localhost:8443  ·  admin / admin
- **realm**: `toss`  ·  issuer `https://localhost:8443/realms/toss`
- **테스트 사용자**: `tester` / `test1234`

## clients

| clientId | 타입 | flow | 용도 |
|----------|------|------|------|
| `toss-web-bff` | confidential (secret) | Auth Code + PKCE | 웹 SPA용 BFF (백엔드가 secret 보유) |
| `toss-android` | public | Auth Code + PKCE | Android 네이티브 (AppAuth) |
| `toss-ios` | public | Auth Code + PKCE | iOS 네이티브 (AppAuth) |

## 토큰 정책 (production급)

| 항목 | 값 | 의미 |
|------|-----|------|
| `accessTokenLifespan` | 300s | access token 짧게 |
| `revokeRefreshToken` + `refreshTokenMaxReuse: 0` | on | **refresh token rotation** (재사용 차단) |
| `ssoSessionIdleTimeout` | 1800s | 유휴 30분 후 세션 만료 |
| `ssoSessionMaxLifespan` | 36000s | 세션 최대 10시간 |

> 모든 secret/비밀번호는 **로컬 개발용 더미**다. 운영에선 반드시 교체.
