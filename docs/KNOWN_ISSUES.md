# 알려진 이슈 (Known Issues)

추적 중인 결함. 각 항목은 증상·범위·원인·**완벽 개선 목표**를 명세한다.

---

## KI-1 · 로그아웃 후 UI 에 사용자 정보가 남음 (web + mobile)

- **상태:** ✅ 해결 (2026-06-25)
- **증상(과거):** 로그아웃해도 UI 에 사용자 정보가 남고, 재로그인 시 비밀번호 없이 silent SSO 로 같은 계정 자동 로그인.
- **범위:** web(`web/src/lib/auth.ts`) + backend(`SecurityConfig`) + mobile(`mobile/src/auth/oidc.ts`) + Keycloak(`toss-realm.json`).

### 해결 — RP-initiated end-session 완성
1. **UI 결정적 리셋** — web: `/logout` 후 `window.location.assign(endSessionUrl)` 전체 전환 → `useMe` 401 → 로그인 화면. mobile: Keychain clear → `removeQueries(['me'])` → 401.
2. **Keycloak SSO 세션 종료(핵심)** — 백엔드 `/logout` 이 302 대신 **end-session URL(id_token_hint 포함)을 JSON** 으로 반환(`JsonEndSessionLogoutSuccessHandler`). fetch 는 CSRF 헤더로 호출하고, 받은 URL 로 **브라우저를 네비게이트**해 SSO 쿠키까지 종료 후 `post_logout_redirect_uri` 로 복귀. mobile 은 app-auth `logout()` 로 동일.
3. **Keycloak post-logout URI 등록** — web `http://localhost:5173/*`(기존), mobile `com.toss.app://oauth2redirect`(추가).
4. **검증** — 로그아웃 → 재로그인 시 **비밀번호 요구**(silent SSO 없음). web·mobile 공통 정책.

> 후속: 프론트 **페이지별 인가 가드**(미인증·권한부족 시 콘텐츠 비렌더)는 별도 UI/보안 강화 작업에서 마무리.
