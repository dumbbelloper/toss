# 엔지니어링 — Web (React + Vite + Tailwind 4 + TanStack)

> 빠른 실행은 [`web/README.md`](../../web/README.md). 이 문서는 심화·결정 사항.

## 역할
- SPA. 인증은 **BFF httpOnly 세션 쿠키**(백엔드가 OIDC 중개). 토큰을 JS에서 다루지 않음.
- API: TanStack Query. 인증 헬퍼 `web/src/lib/auth.ts`(모바일 `src/auth/auth.ts` 와 미러).

## 다가오는 작업 (PRODUCT 기둥)
- 자산관리 페이지(Toss API + 실시간 SSE).
- 백테스팅 페이지, 은퇴 시뮬레이터 페이지.
- 차트: d3-scale/d3-shape (mobile 과 수학 공유).
- 디자인 토큰 공유 소비 — `docs/design/`.

## TODO
- [ ] 공유 디자인 토큰 연결
- [ ] 페이지 라우팅 구조(기둥별)
