// OIDC 토큰의 보안 저장소. iOS Keychain / Android Keystore 를 react-native-keychain 으로
// 추상화한다. 토큰은 평문 AsyncStorage 가 아니라 OS 보안 저장소에 둔다.

import * as Keychain from 'react-native-keychain';

const SERVICE = 'com.tossmobile.oidc';

/** app-auth 결과에서 보관이 필요한 부분만 추린 형태. */
export interface SavedSession {
  accessToken: string;
  /** ISO8601. access token 만료 시각(갱신 판단에 사용). */
  accessTokenExpirationDate: string;
  refreshToken: string;
  /** RP-initiated logout(end-session)의 id_token_hint 용. */
  idToken: string;
}

export async function saveSession(session: SavedSession): Promise<void> {
  await Keychain.setGenericPassword('oidc', JSON.stringify(session), { service: SERVICE });
}

export async function loadSession(): Promise<SavedSession | null> {
  const creds = await Keychain.getGenericPassword({ service: SERVICE });
  if (!creds) {
    return null;
  }
  try {
    return JSON.parse(creds.password) as SavedSession;
  } catch {
    // 저장 포맷이 깨졌으면 없는 것으로 취급(다음 로그인에서 덮어씀).
    return null;
  }
}

export async function clearSession(): Promise<void> {
  await Keychain.resetGenericPassword({ service: SERVICE });
}
