import { Link, Outlet } from '@tanstack/react-router'

import { isUnauthorized, useLogout, useMe } from '../lib/auth'
import { LogoMark } from './Logo'

export function RootLayout() {
  const me = useMe()
  const logout = useLogout()

  return (
    <div className="min-h-full bg-bg text-ink">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-5">
            <Link to="/" className="flex items-center gap-2 text-lg font-bold text-gray-900">
              <LogoMark size={22} />
              곳간
            </Link>
            {me.isSuccess && (
              <nav className="flex items-center gap-4 text-sm text-gray-500">
                <Link to="/" className="hover:text-gray-900 [&.active]:font-semibold [&.active]:text-gray-900">
                  홈
                </Link>
                <Link
                  to="/portfolio"
                  className="hover:text-gray-900 [&.active]:font-semibold [&.active]:text-gray-900"
                >
                  자산 관리
                </Link>
                <Link
                  to="/backtest"
                  className="hover:text-gray-900 [&.active]:font-semibold [&.active]:text-gray-900"
                >
                  백테스팅
                </Link>
                <Link
                  to="/simulator"
                  className="hover:text-gray-900 [&.active]:font-semibold [&.active]:text-gray-900"
                >
                  시뮬레이터
                </Link>
              </nav>
            )}
          </div>
          <nav className="flex items-center gap-3 text-sm">
            {me.isSuccess && (
              <>
                <span className="text-gray-500">{me.data.username}</span>
                <button
                  type="button"
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                  className="rounded-md border border-gray-300 px-3 py-1.5 font-medium hover:bg-gray-100 disabled:opacity-50"
                >
                  로그아웃
                </button>
              </>
            )}
            {isUnauthorized(me.error) && (
              <span className="text-gray-400">로그인 필요</span>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}
