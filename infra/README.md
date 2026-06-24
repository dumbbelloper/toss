# infra

로컬 개발용 인증 인프라.

- `keycloak/` — Keycloak realm·client export(JSON). 재현 가능한 구성을 위해 버전관리한다.
- docker-compose로 Keycloak + Postgres를 production급 설정(`start` 모드, TLS, refresh token rotation)으로 기동.

> 구성 예정 (Task #2).
