import { Link, Outlet } from '@tanstack/react-router'

import { isUnauthorized, login, useLogout, useMe } from '../lib/auth'
import { LogoMark } from './Logo'

const NAV = [
  { to: '/', label: '홈' },
  { to: '/portfolio', label: '자산 관리' },
  { to: '/backtest', label: '백테스팅' },
  { to: '/simulator', label: '시뮬레이터' },
] as const

export function RootLayout() {
  const me = useMe()
  const logout = useLogout()

  return (
    <div className="min-h-full bg-bg text-ink">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-5">
            <Link to="/" className="flex items-center gap-2 text-lg font-bold text-ink">
              <LogoMark size={22} />
              곳간
            </Link>
            {me.isSuccess && (
              <nav className="flex items-center gap-4 text-sm text-muted">
                {NAV.map((n) => (
                  <Link
                    key={n.to}
                    to={n.to}
                    className="hover:text-ink [&.active]:font-semibold [&.active]:text-accent"
                  >
                    {n.label}
                  </Link>
                ))}
              </nav>
            )}
          </div>
          <nav className="flex items-center gap-3 text-sm">
            {me.isSuccess && (
              <>
                <span className="text-muted">{me.data.username}</span>
                <button
                  type="button"
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                  className="rounded-md border border-line px-3 py-1.5 font-medium text-ink hover:bg-bg disabled:opacity-50"
                >
                  로그아웃
                </button>
              </>
            )}
            {isUnauthorized(me.error) && (
              <button
                type="button"
                onClick={login}
                className="rounded-md bg-accent px-4 py-1.5 font-semibold text-white hover:bg-accent-hover"
              >
                로그인
              </button>
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
