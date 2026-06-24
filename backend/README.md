# toss

토스증권 Open API 를 활용한 주식 모니터링·매매(매수/매도) 사이드 프로젝트.

## 기술 스택

- **런타임**: Java 25, Spring Boot 4.1 (가상 스레드 기반 동기 처리)
- **Toss 연동**: 선언형 HTTP 클라이언트(`@HttpExchange`) + `RestClient`, OAuth2 Client Credentials
- **복원력**: 클라이언트측 레이트리밋(토큰 버킷, API 그룹별) + Spring FW7 네이티브 `@Retryable`
- **영속성**: PostgreSQL 17 + Spring Data JDBC + Flyway
- **대시보드(예정)**: Thymeleaf + HTMX + SSE
- **알림(예정)**: Telegram Bot

API 명세 원본은 [`docs/api/`](../docs/api) 에 보관(`openapi.json` 이 단일 진실 공급원).

## 사전 준비

- **Docker** — 로컬 PostgreSQL 컨테이너용 (`backend/compose.yaml`, 직접 `docker compose up -d` 로 기동)
- JDK 는 별도 설치 불필요 — Gradle 툴체인이 Java 25 를 자동 프로비저닝

## 설정

자격증명은 **`src/main/resources/application-local.yml`** 에 둔다(이 파일은 `.gitignore` 처리되어 커밋되지 않음):

```yaml
toss:
  client-id: <발급받은 client_id>       # WTS > 설정 > Open API
  client-secret: <발급받은 client_secret>
  # account-seq: 1                       # 계좌·주문 API 사용 시 (GET /api/v1/accounts 로 확인)
```

운영 환경에서는 환경변수(`TOSS_CLIENT_ID`, `TOSS_CLIENT_SECRET`, `SPRING_DATASOURCE_*`)로 주입한다.

## 실행

```bash
docker compose up -d   # 로컬 PostgreSQL 기동 (backend/compose.yaml)
./gradlew bootRun
```

DB 접속 정보(`jdbc:postgresql://localhost:5432/toss`, `toss`/`toss`)는 `application.yml` 에
기본값으로 명시돼 있다. 테스트는 Testcontainers 가, 운영은 `SPRING_DATASOURCE_*` 환경변수가
이 값을 오버라이드한다.

### 연동 스모크 체크 (선택)

기동 시 현재가를 1회 조회해 토큰→레이트리밋→호출→파싱 전 과정을 검증한다(실패해도 기동은 계속):

```bash
./gradlew bootRun --args='--toss.smoke.enabled=true --toss.smoke.symbol=005930'
```

## 인증 (BFF)

이 백엔드는 웹 SPA 의 **BFF** 다. Keycloak(OIDC) confidential client 로서 로그인 흐름을
중개하고, 브라우저엔 httpOnly 세션 쿠키만 내려간다(토큰은 서버 보관). 자세한 배경은
루트 [`README.md`](../README.md) 참고.

**사전 준비**: Keycloak 기동([`infra/`](../infra)) + JVM 트러스트(아래) 가 필요하다.

```bash
docker compose up -d                                  # 앱 PostgreSQL (backend/compose.yaml)
docker compose -f ../infra/docker-compose.yml up -d   # Keycloak + Keycloak 전용 DB
../infra/keycloak/trust-jvm.sh                          # 백엔드→Keycloak HTTPS 신뢰(1회)
./gradlew bootRun
```

> 앱 DB(`backend/compose.yaml`)와 Keycloak 전용 DB(`infra/docker-compose.yml` 의 `keycloak-db`)는
> 서로 분리된 별도 컨테이너다.

bootRun 은 `../infra/keycloak/tls/dev-truststore.p12` 가 있으면 자동으로 JVM 에 주입한다.

### IntelliJ 에서 실행할 때 (중요)

IntelliJ 의 ▶ 실행 버튼(Spring Boot Application 구성)은 `bootRun` 을 거치지 않고 `main` 을
직접 띄우므로, 위 트러스트스토어가 **주입되지 않는다**. 그 상태로 실행하면 기동 중 Keycloak
issuer discovery 에서 다음 에러로 죽는다:

```
Unable to resolve Configuration with the provided Issuer of "https://localhost:8443/realms/toss"
  → SSLHandshakeException: PKIX path building failed: unable to find valid certification path
```

해결: 실행 구성의 **VM options** 에 트러스트스토어를 직접 지정한다.
`Run → Edit Configurations… → (해당 구성) → Modify options → Add VM options` 체크 후 입력:

```
-Djavax.net.ssl.trustStore=/절대경로/toss/infra/keycloak/tls/dev-truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit
```

> 참고
> - VM options 칸은 `$PROJECT_DIR$` 같은 매크로를 치환하지 않으므로 **절대경로**로 적는다.
> - Gradle 툴체인이 JDK 25 를 쓰므로, `Settings → Build Tools → Gradle → Gradle JVM` 도 **JDK 25**
>   로 맞춘다(시스템 기본이 다른 버전이면 import/빌드가 깨질 수 있음).
> - 또는 ▶ 대신 Gradle 패널의 `application → bootRun` 으로 실행하면 트러스트스토어가 자동 주입된다.

| 엔드포인트 | 동작 |
|-----------|------|
| `GET /api/me` | 인증 시 사용자 정보(JSON), 미인증 시 **401** |
| `GET /oauth2/authorization/keycloak` | 로그인 시작 → Keycloak 으로 302 |
| `POST /logout` | 세션 종료 + Keycloak RP-initiated logout (CSRF 토큰 필요) |

- OAuth2 client 설정: `application.yml` 의 `spring.security.oauth2.client`
- 보안 구성: `com.toss.security` (`SecurityConfig`, SPA CSRF 핸들러, `/api/me`)
- 미인증 시 리다이렉트 대신 **401** 을 반환한다 — SPA(fetch)가 받아 로그인을 시작하기 위함.

## 테스트

```bash
./gradlew test     # 단위 + 통합(Testcontainers PostgreSQL, Docker 필요)
```

`@SpringBootTest` 는 Keycloak 없이 로드되도록 `src/test/resources/application.yml` 에
정적 OAuth2 엔드포인트를 주입한다(issuer discovery 네트워크 호출 회피).

## 패키지 구조

```
com.toss
├─ config     설정 (TossProperties, RestClient, Resilience)
├─ auth       OAuth2 토큰 관리 + Bearer 인터셉터
├─ client     @HttpExchange 클라이언트 + DTO
├─ ratelimit  API 그룹별 토큰 버킷 레이트리미터
├─ common     공통 예외(TossApiException)
├─ service    도메인 서비스 (envelope 해제 + 에러 매핑 + 재시도)
└─ smoke      기동 시 연동 검증 러너
```

## 레이트 리밋 / 재시도 설정 (선택)

`application.yml` 또는 환경변수로 조정 가능 (기본값):

```yaml
toss:
  retry:
    max-retries: 3       # 429/5xx 재시도 횟수
    delay-ms: 1000       # 초기 백오프
    multiplier: 2.0      # 지수 배수
    max-delay-ms: 8000
    jitter-ms: 250
```
