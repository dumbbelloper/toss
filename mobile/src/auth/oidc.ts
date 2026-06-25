// Keycloak 과의 OIDC 흐름(Authorization Code + PKCE)을 react-native-app-auth 로 처리한다.
// 로그인은 시스템 브라우저(Custom Tab / ASWebAuthenticationSession)에서 진행되고,
// 결과 토큰은 Keychain 에 저장한다. API 호출 직전엔 getFreshAccessToken 으로 만료를 점검·갱신한다.

import {
  authorize,
  logout,
  refresh,
  revoke,
  type AuthConfiguration,
  type AuthorizeResult,
  type RefreshResult,
} from 'react-native-app-auth';

import { OIDC } from '../config';
import { clearSession, loadSession, saveSession, type SavedSession } from './tokens';

const config: AuthConfiguration = {
  issuer: OIDC.issuer,
  clientId: OIDC.clientId,
  redirectUrl: OIDC.redirectUrl,
  scopes: [...OIDC.scopes],
  // public client + PKCE(S256). app-auth 는 기본적으로 PKCE 를 사용한다.
};

/** access token 갱신 여유(만료 30초 전부터는 미리 refresh). */
const EXPIRY_SKEW_MS = 30_000;

function toSession(result: AuthorizeResult | RefreshResult, prev?: SavedSession): SavedSession {
  return {
    accessToken: result.accessToken,
    accessTokenExpirationDate: result.accessTokenExpirationDate,
    // refresh 응답은 refreshToken/idToken 을 생략할 수 있어 이전 값을 보존한다.
    refreshToken: result.refreshToken ?? prev?.refreshToken ?? '',
    idToken: result.idToken ?? prev?.idToken ?? '',
  };
}

/**
 * 사용자가 로그인 시트를 "취소" 했는지 판별한다.
 *
 * react-native-app-auth(iOS)는 취소를 별도 코드로 구분하지 않고 `authentication_failed` 로
 * 던지며, 메시지는 "The operation couldn't be completed ..."(general 도메인 -3) 이다.
 * Android 는 "User cancelled flow". 둘 다 메시지로 감지한다. 취소는 에러가 아니다.
 */
function isUserCancellation(err: unknown): boolean {
  const message = ((err as { message?: string })?.message ?? '').toLowerCase();
  return (
    message.includes('cancel') ||
    message.includes("operation couldn't be completed") ||
    message.includes('operation couldn’t be completed')
  );
}

/**
 * 시스템 브라우저로 로그인하고 토큰을 저장한다. 성공 시 세션을 반환.
 * 사용자가 취소하면 에러가 아니라 `null` 을 반환(화면은 그냥 로그인 상태 유지).
 */
export async function signInWithKeycloak(): Promise<SavedSession | null> {
  try {
    const result = await authorize(config);
    const session = toSession(result);
    await saveSession(session);
    return session;
  } catch (err) {
    if (isUserCancellation(err)) {
      return null;
    }
    throw err;
  }
}

/**
 * 유효한 access token 을 반환한다. 만료가 임박하면 refresh 로 갱신한다.
 * 세션이 없거나 갱신 실패면 세션을 비우고 null 을 반환(= 로그아웃 상태).
 */
export async function getFreshAccessToken(): Promise<string | null> {
  const session = await loadSession();
  if (!session) {
    return null;
  }

  const expiresAt = new Date(session.accessTokenExpirationDate).getTime();
  if (Number.isFinite(expiresAt) && expiresAt - EXPIRY_SKEW_MS > Date.now()) {
    return session.accessToken;
  }

  if (!session.refreshToken) {
    await clearSession();
    return null;
  }

  try {
    const refreshed = await refresh(config, { refreshToken: session.refreshToken });
    const updated = toSession(refreshed, session);
    await saveSession(updated);
    return updated.accessToken;
  } catch {
    // refresh token 만료/취소 → 재로그인 필요.
    await clearSession();
    return null;
  }
}

/**
 * 로그아웃을 결정적·완전하게 처리한다.
 * 1) 로컬 토큰(Keychain) 제거 → 화면 즉시 로그아웃 전환.
 * 2) refresh token best-effort revoke.
 * 3) Keycloak end-session(RP-initiated): 시스템 브라우저로 SSO 세션 쿠키까지 종료한다.
 *    안 하면 재로그인이 silent SSO 로 같은 계정 자동 로그인됨(KI-1).
 */
export async function signOut(): Promise<void> {
  const session = await loadSession();
  await clearSession();
  if (session?.refreshToken) {
    try {
      await revoke(config, { tokenToRevoke: session.refreshToken, sendClientId: true });
    } catch {
      // revoke 실패는 무시 — 로컬 토큰은 이미 제거됨.
    }
  }
  if (session?.idToken) {
    try {
      await logout(config, {
        idToken: session.idToken,
        postLogoutRedirectUrl: OIDC.redirectUrl,
      });
    } catch {
      // 사용자가 브라우저 시트를 닫는 등 — 로컬 토큰은 이미 제거됨(로그아웃은 유효).
    }
  }
}
