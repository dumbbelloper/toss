# web

React + Vite + Tailwind 4 + TanStack 기반 SPA.

인증은 **BFF 패턴**: 이 앱은 Keycloak을 직접 호출하지 않고 백엔드 엔드포인트(`/api/*`, `/oauth2/authorization/keycloak`)만 사용한다. 토큰은 브라우저에 저장되지 않으며 httpOnly 세션 쿠키로 처리된다.

> 스캐폴드 예정 (Task #4).
