// 인증 상태 훅. 웹의 web/src/lib/auth.ts 를 모바일용으로 미러링한다.
// useMe = 로그인 여부 쿼리, useLogin/useLogout = 명령형 흐름(브라우저 로그인 / 토큰 정리).

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, fetchMe } from '../api/client';
import { signInWithKeycloak, signOut } from './oidc';

/**
 * 로그인 상태 훅. 401 은 "로그아웃 상태"라는 정상 결과이므로 재시도하지 않고 그대로 노출한다
 * (화면이 401 → 로그인 버튼 표시).
 */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 2,
    staleTime: 60_000,
  });
}

export function isUnauthorized(err: unknown): boolean {
  return err instanceof ApiError && err.status === 401;
}

/** 로그인 시작: 시스템 브라우저로 Keycloak PKCE 흐름을 수행한다. */
export function useLogin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: signInWithKeycloak,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me'] });
    },
  });
}

/**
 * 로그아웃: 로컬 토큰 정리(+백그라운드 revoke) 후 me 쿼리를 **reset**.
 * removeQueries 는 마운트된 useMe 옵저버를 즉시 리렌더하지 못해 화면이 멈춰 보였다 →
 * resetQueries 로 초기상태+refetch 를 강제해 곧바로 미인증(랜딩) 으로 전환한다.
 */
export function useLogout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: signOut,
    onSuccess: () => {
      qc.resetQueries({ queryKey: ['me'] });
    },
  });
}
