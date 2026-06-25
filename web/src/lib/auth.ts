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
      // 백엔드 세션을 무효화한다(헤더 CSRF). 백엔드는 Keycloak end-session 으로 302(cross-origin)를
      // 응답하므로 redirect:'manual' 로 받아 넘긴다.
      await fetch('/logout', {
        method: 'POST',
        credentials: 'include',
        redirect: 'manual',
        headers: xsrf ? { 'X-XSRF-TOKEN': xsrf } : undefined,
      })
      // 캐시·관찰자 잔존(=로그아웃 후 정보 노출)을 원천 차단: 전체 새로고침 → useMe 가 401 을
      // 받아 로그인 화면으로 전환된다. (KI-1 의 web 부분)
      window.location.assign('/')
    },
  })
}
