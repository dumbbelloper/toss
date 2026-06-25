// 백엔드(resource-server) 호출 래퍼. 웹의 web/src/lib/api.ts 를 모바일용으로 미러링한다.
// 차이: 웹은 동일 출처 쿠키 세션이지만, 모바일은 매 요청에 Bearer JWT 를 직접 싣는다.

import { API_BASE } from '../config';
import { getFreshAccessToken } from '../auth/oidc';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** GET /api/me 응답(백엔드 AuthController 와 동일 형태). */
export interface Me {
  subject: string;
  username: string;
  email: string | null;
  name: string | null;
  roles: string[];
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await getFreshAccessToken();
  const headers = new Headers(init.headers);
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const method = (init.method ?? 'GET').toUpperCase();
  const res = await fetch(API_BASE + path, { ...init, method, headers });
  if (!res.ok) {
    throw new ApiError(res.status, `${method} ${path} → ${res.status}`);
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/** 현재 사용자. 미인증이면 ApiError(401) 를 던진다. */
export function fetchMe(): Promise<Me> {
  return api<Me>('/api/me');
}

export function isUnauthorized(err: unknown): boolean {
  return err instanceof ApiError && err.status === 401;
}
