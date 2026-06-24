// Keycloak 과의 OIDC 흐름(Authorization Code + PKCE)을 react-native-app-auth 로 처리한다.
// 로그인은 시스템 브라우저(Custom Tab / ASWebAuthenticationSession)에서 진행되고,
// 결과 토큰은 Keychain 에 저장한다. API 호출 직전엔 getFreshAccessToken 으로 만료를 점검·갱신한다.

import {
  authorize,
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

/** 시스템 브라우저로 로그인하고 토큰을 저장한다. 성공 시 세션을 반환. */
export async function signInWithKeycloak(): Promise<SavedSession> {
  const result = await authorize(config);
  const session = toSession(result);
  await saveSession(session);
  return session;
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

/** 로컬 세션을 비우고, refresh token 을 best-effort 로 revoke 한다. */
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
}
