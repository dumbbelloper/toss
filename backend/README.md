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

- **Docker** (로컬 PostgreSQL 컨테이너용 — `spring-boot-docker-compose` 가 자동 기동)
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
# PostgreSQL 은 bootRun 시 compose.yaml 로 자동 기동/중지된다.
./gradlew bootRun
```

### 연동 스모크 체크 (선택)

기동 시 현재가를 1회 조회해 토큰→레이트리밋→호출→파싱 전 과정을 검증한다(실패해도 기동은 계속):

```bash
./gradlew bootRun --args='--toss.smoke.enabled=true --toss.smoke.symbol=005930'
```

## 테스트

```bash
./gradlew test     # 단위 + 통합(Testcontainers PostgreSQL, Docker 필요)
```

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
