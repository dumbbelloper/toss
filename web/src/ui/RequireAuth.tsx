// 페이지별 인가 가드. 미인증이면 랜딩(/)으로, 권한 부족이면 차단 화면을 렌더한다.
// (실제 보안은 백엔드가 401/403 으로 강제 — 이건 UX 가드.)
import { Navigate } from '@tanstack/react-router'
import { type ReactNode } from 'react'

import { isUnauthorized, useMe } from '../lib/auth'

export function RequireAuth({ children, roles }: { children: ReactNode; roles?: string[] }) {
  const me = useMe()

  if (me.isLoading) {
    return <p className="text-muted">불러오는 중…</p>
  }
  if (isUnauthorized(me.error)) {
    return <Navigate to="/" />
  }
  if (me.isError) {
    return <p className="text-loss">오류: {String(me.error)}</p>
  }
  if (roles && me.data && !roles.some((r) => me.data!.roles.includes(r))) {
    return (
      <section className="rounded-xl border border-line bg-surface p-8 text-center">
        <h1 className="text-xl font-bold text-ink">접근 권한이 없습니다</h1>
        <p className="mt-2 text-sm text-muted">이 페이지는 {roles.join(', ')} 권한이 필요합니다.</p>
      </section>
    )
  }
  return <>{children}</>
}
