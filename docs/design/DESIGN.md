# 디자인 시스템

> web(React)·mobile(React Native) **통일 UI**의 단일 출처.
> 원칙: 컴포넌트 코드는 플랫폼별로 다를 수 있어도, **토큰(색·타이포·간격·라운드·모션)은 공유한다.**

## 통일 전략

- **Design Tokens 단일 출처** — 플랫폼 무관 TS/JSON 토큰을 모노레포 공유 패키지(`packages/design-tokens`
  또는 유사 위치)에 두고 web·mobile 이 함께 소비. 토큰 정의는 `tokens.md` 와 코드가 미러.
- 컴포넌트는 **계약(Props·동작) 공유 + 플랫폼별 구현**. (한 단계 더: Tamagui 로 RN+web 통합 가능 — 추후 검토)
- 상태: `components.md` 에 web/mobile 패리티 추적.

## TODO (검증되면 채움)

- [ ] 디자인 원칙(톤·무드) 1줄 정의 — "개인 자산 대시보드: 정보 밀도 높되 차분하게"
- [ ] 컬러 팔레트(라이트/다크, 수익=빨강·손실=파랑? 한국 관습 확인)
- [ ] 타이포 스케일 / 숫자 표기(금액·% 정렬, tabular-nums)
- [ ] 간격·라운드·그림자 스케일
- [ ] 차트 색/축 규칙(`docs/product/simulators.md` 와 연결)
- [ ] 토큰 패키지 위치·빌드 방식 확정 → `docs/adr/`

> 참고: gstack `/design-consultation` 스킬로 이 문서를 부트스트랩할 수 있음(현재 미실행).
