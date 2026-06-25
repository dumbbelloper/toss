# 엔지니어링 — Backend (Spring Boot, Java 25)

> 빠른 실행은 [`backend/README.md`](../../backend/README.md). 이 문서는 심화·결정 사항.

## 역할
- 사용자 인증: 웹 **BFF**(confidential client, code flow 중개, 브라우저에 토큰 미노출),
  모바일 **Bearer JWT**(resource-server 검증). 근거: 루트 README 아키텍처 절 + `docs/adr/`.
- Toss 연동: 백엔드만 `client_secret` 보유(machine-to-machine). 스펙은 [`docs/api/`](../api).
- 데이터: Postgres + Spring Data JDBC.

## 다가오는 작업 (PRODUCT 기둥)
- 인가(authz/roles) 3-플랫폼 정합성 점검.
- Toss API 20개 엔드포인트 연동(@HttpExchange + RestClient) — `docs/api/`.
- 백테스트 데이터 적재 잡 + `price_daily`/`macro_series` — `docs/data/sources.md`.

## TODO
- [ ] @HttpExchange 클라이언트 골격
- [ ] 시세 적재 @Scheduled 잡 + provider 어댑터
- [ ] 백테스트 엔진 서비스
