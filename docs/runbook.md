# 런북 (배포 · 장애 대응)

> "무언가 잘못됐을 때 뭘 하나" 의 절차. 검증된 것만 적는다(추측 금지).

## 로컬 기동 (개발)

| 대상 | 방법 | 문서 |
|------|------|------|
| Keycloak + Postgres | `infra/docker-compose.yml` | `infra/README.md` |
| backend (:8080) | Gradle bootRun | `backend/README.md` |
| web SPA | pnpm dev | `web/README.md` |
| mobile | Metro + run-android/ios | `docs/engineering/mobile.md` |

## TODO (정식 배포 시 채움)
- [ ] 배포 타깃·파이프라인 (gstack `/setup-deploy` 로 구성 가능)
- [ ] 헬스체크 엔드포인트
- [ ] 롤백 절차
- [ ] 데이터 적재 잡 실패 시 대응(`docs/data/sources.md`)
