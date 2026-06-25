// 모바일 인증·API 설정의 단일 출처.
//
// 호스트 접근: Android 에뮬레이터는 호스트의 localhost 를 직접 보지 못한다. 대신
// `adb reverse tcp:8443 tcp:8443` / `adb reverse tcp:8080 tcp:8080` 로 에뮬레이터의
// localhost 를 호스트로 포워딩한다. 그러면 앱이 백엔드 설정과 **동일하게** localhost 를
// 쓰므로 Keycloak issuer(https://localhost:8443/realms/toss)와 mkcert 인증서 CN(localhost)이
// 모두 일치한다. (10.0.2.2 를 쓰면 issuer·인증서 호스트네임이 어긋난다.)
//
// 운영 빌드에서는 실제 도메인으로 교체한다(추후 env/flavor 분리).

export const OIDC = {
  /** Keycloak realm issuer. react-native-app-auth 가 여기서 OIDC discovery 를 수행한다. */
  issuer: 'https://localhost:8443/realms/toss',
  /** public client (secret 없음, Authorization Code + PKCE). realm import 와 일치. */
  clientId: 'toss-mobile',
  /** 네이티브 리다이렉트. AndroidManifest 의 appAuthRedirectScheme(com.toss.app)와 일치해야 한다. */
  redirectUrl: 'com.toss.app://oauth2redirect',
  scopes: ['openid', 'profile', 'email'],
} as const;

/** 백엔드(BFF/resource-server). 모바일은 Bearer JWT 로 /api/** 를 호출한다. */
export const API_BASE = 'http://localhost:8080';
