import { useMutation, useQuery } from '@tanstack/react-query'

import { ApiError, api, readCookie } from './api'

export interface Me {
  subject: string
  username: string
  email: string | null
  name: string | null
  roles: string[]
}

/** 현재 사용자. 미인증이면 ApiError(401) 를 던진다. */
export async function fetchMe(): Promise<Me> {
  return api<Me>('/api/me')
}

/**
 * 로그인 상태 훅. 401 은 "로그아웃 상태"라는 정상 결과이므로 재시도하지 않고
 * 그대로 노출한다(컴포넌트가 401 → 로그인 버튼 표시).
 */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 2,
    staleTime: 60_000,
  })
}

export function isUnauthorized(err: unknown): boolean {
  return err instanceof ApiError && err.status === 401
}

/** 로그인 시작: 백엔드가 Keycloak 으로 리다이렉트한다(full-page navigation). */
export function login(): void {
  window.location.href = '/oauth2/authorization/keycloak'
}

/**
 * 로그아웃. 백엔드 세션을 종료한다. 백엔드는 Keycloak end-session 으로 302 를
 * 응답하므로(cross-origin) redirect: 'manual' 로 받아 넘기고, 로컬 상태만 정리한다.
 */
export function useLogout() {
  return useMutation({
    mutationFn: async () => {
      const xsrf = readCookie('XSRF-TOKEN')
      // 백엔드 세션을 무효화한다(헤더 CSRF). 백엔드는 Keycloak end-session URL 을 JSON 으로 돌려준다.
      const res = await fetch('/logout', {
        method: 'POST',
        credentials: 'include',
        headers: xsrf ? { 'X-XSRF-TOKEN': xsrf } : undefined,
      })
      const data = res.ok ? await res.json().catch(() => null) : null
      // 받은 end-session URL 로 브라우저를 네비게이트 → Keycloak SSO 세션까지 종료하고
      // post_logout_redirect_uri(=SPA 루트)로 복귀한다. useMe 가 401 → 로그인 화면.
      // (KI-1: SSO 세션 미종료로 재로그인이 silent 되던 문제 해결)
      window.location.assign(data?.logoutUrl ?? '/')
    },
  })
}
