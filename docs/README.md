# docs — 프로젝트 문서 체계

기획·디자인·개발·데이터·운영을 한 곳에서 관리한다. 각 문서는 자기 영역의 **단일 진실 공급원**.

## 인덱스

| 영역 | 문서 | 내용 |
|------|------|------|
| 기획 | [`PRODUCT.md`](PRODUCT.md) | 비전·사용자·기둥(자산관리/백테스팅/은퇴시뮬)·원칙·Parked |
| 디자인 | [`design/DESIGN.md`](design/DESIGN.md) · [`tokens.md`](design/tokens.md) · [`components.md`](design/components.md) | web/mobile 통일 UI, 디자인 토큰, 컴포넌트 패리티 |
| 제품 스펙 | [`product/simulators.md`](product/simulators.md) | retiremoni 분석, 시뮬레이터·백테스트 엔진 스펙 |
| 데이터 | [`data/sources.md`](data/sources.md) | 백테스트 데이터 소스·라이선스·적재 스키마 |
| 엔지니어링 | [`engineering/`](engineering) | [mobile](engineering/mobile.md) · [backend](engineering/backend.md) · [web](engineering/web.md) |
| API | [`api/`](api) | 토스 Open API 명세(단일 출처) |
| 운영 | [`runbook.md`](runbook.md) | 배포·장애 대응 |
| 이슈 | [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) | 추적 중인 결함(예: KI-1 로그아웃 UI) |
| 결정 | [`adr/`](adr) | 아키텍처 결정 기록 |

## 작성 원칙
- **검증된 것만 적는다.** 추측 절차는 TODO로 표시.
- 기획(why)은 여기, 실행(how)의 빠른 버전은 각 폴더 README, 심화는 `engineering/`.
- 중요한 선택은 `adr/` 에 짧게 근거를 남긴다.
