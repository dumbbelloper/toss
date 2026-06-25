# 알려진 이슈 (Known Issues)

추적 중인 결함. 각 항목은 증상·범위·원인·**완벽 개선 목표**를 명세한다.

---

## KI-1 · 로그아웃 후 UI 에 사용자 정보가 남음 (web + mobile)

- **상태:** 열림 (추후 완벽 개선 — 사용자 지시 2026-06-25)
- **증상:** 로그아웃해도 화면에 로그인 사용자 정보/네비/보호 콘텐츠가 그대로 노출된다.
- **범위:** web(`web/src/lib/auth.ts` `useLogout`) + mobile(`mobile/src/auth/auth.ts` `useLogout`).
  **동일 패턴**이라 한 곳을 고치면 양쪽 같은 정책으로 가야 한다.

### 현재 구현 (불충분)
로그아웃 시 `queryClient.removeQueries({ queryKey: ['me'] })` 로 **'me' 캐시만 제거**한다.
- web: `/logout` POST(`redirect: 'manual'`) 호출 후 캐시 제거.
- mobile: `signOut`(Keychain 토큰 clear + best-effort revoke) 후 캐시 제거.

### 추정 원인
- **캐시 제거 ≠ 상태 전환.** mounted `useMe` 관찰자가 즉시 "로그아웃"으로 전환되지 않거나,
  재요청이 (세션 미종료 시) 200 을 받아 사용자 정보가 유지된다.
- **web:** 백엔드 세션/`/logout`(CSRF·redirect:manual) 처리가 UI 에 결정적으로 반영 안 됨.
- **mobile:** Keychain 은 비워지나 화면 상태 전환이 즉시·결정적이지 않음. 브라우저 end-session 미처리.

### 완벽 개선 목표 (추후)
로그아웃을 **결정적·완전**하게:
1. **서버/토큰 세션 확실 종료** (web: 백엔드 세션 무효화 확인 / mobile: 토큰 폐기).
2. **Keycloak RP-initiated end-session** 으로 **SSO 세션까지 종료** — 안 하면 재로그인 시
   silent SSO 로 같은 계정이 자동 로그인되어 "로그아웃이 안 된 것처럼" 보인다.
3. **클라이언트 인증 상태를 결정적으로 '로그아웃'으로 리셋** (낙관적 업데이트: 서버 응답 전에
   UI 를 즉시 로그아웃 상태로). 단순 캐시 제거가 아니라 명시적 상태 전환.
4. **보호 라우트/화면 가드** — 미인증이면 콘텐츠를 렌더하지 않음.
5. web/mobile **공통 정책**으로 일관되게.
