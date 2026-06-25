import { Link } from '@tanstack/react-router'

import { isUnauthorized, useMe, type Me } from '../lib/auth'
import { LandingPage } from './LandingPage'

export function HomePage() {
  const me = useMe()

  if (me.isLoading) {
    return <p className="text-muted">불러오는 중…</p>
  }
  // 미인증 → 랜딩(서비스 소개 + 로그인 CTA)
  if (isUnauthorized(me.error)) {
    return <LandingPage />
  }
  if (me.isError) {
    return <p className="text-loss">오류: {String(me.error)}</p>
  }
  return <Dashboard user={me.data!} />
}

const LINKS = [
  { to: '/portfolio', emoji: '📊', title: '자산 관리', desc: '보유·평가·시세' },
  { to: '/backtest', emoji: '⏳', title: '백테스팅', desc: '전략 과거 검증' },
  { to: '/simulator', emoji: '💰', title: '배당 시뮬레이터', desc: '세후 현금흐름' },
] as const

function Dashboard({ user }: { user: Me }) {
  return (
    <section className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-ink">안녕하세요, {user.name || user.username} 님</h1>
        <p className="mt-1 text-sm text-muted">무엇부터 살펴볼까요?</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        {LINKS.map((l) => (
          <Link
            key={l.to}
            to={l.to}
            className="rounded-2xl border border-line bg-surface p-6 transition hover:border-accent"
          >
            <div className="text-3xl">{l.emoji}</div>
            <h3 className="mt-3 font-bold text-ink">{l.title}</h3>
            <p className="mt-1 text-sm text-muted">{l.desc}</p>
          </Link>
        ))}
      </div>
    </section>
  )
}
