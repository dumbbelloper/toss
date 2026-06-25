// 미인증 사용자용 랜딩 페이지. 서비스 소개 + 기능 + 차별점 + 로그인 CTA.
import { login } from '../lib/auth'
import { LogoMark } from '../ui/Logo'

const FEATURES: { emoji: string; title: string; desc: string }[] = [
  {
    emoji: '📊',
    title: '자산 관리',
    desc: '토스증권 API로 보유 종목·평가금액·시세를 한 화면에. 실시간 손익과 차트.',
  },
  {
    emoji: '⏳',
    title: '백테스팅',
    desc: '미국·한국 ETF 수십 년 데이터로 전략 검증. 배당 재투자·환율·최대낙폭까지 정교하게.',
  },
  {
    emoji: '💰',
    title: '배당 시뮬레이터',
    desc: '실제 분배금 기반 세후 현금흐름. 국내 vs 해외 ETF 세제 비교, 건보료·종합과세 경고.',
  },
]

const EDGES: [string, string][] = [
  ['내부 DB 기반', '백테스트·시뮬은 라이브 호출 없이 내부 DB만 사용 — 빠르고 일관된 결과.'],
  ['한국 세제 정확 반영', 'ETF 3분류 과세·금융소득종합과세·건강보험료까지 교차검증된 규칙으로 계산.'],
  ['통일된 UI', '웹과 모바일(iOS·Android)이 동일한 디자인과 차트 로직.'],
  ['보안 우선', 'BFF httpOnly 세션 + Keycloak SSO. 토큰을 브라우저에 노출하지 않습니다.'],
]

export function LandingPage() {
  return (
    <div className="space-y-16 py-4">
      <section className="flex flex-col items-center text-center">
        <LogoMark size={56} />
        <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-ink">곳간</h1>
        <p className="mt-3 max-w-xl text-lg leading-relaxed text-muted">
          내 자산을 한 곳에. <b className="text-ink">토스증권 연동</b>, <b className="text-ink">정교한 백테스트</b>,
          <b className="text-ink"> 세후 배당 시뮬레이터</b>까지 — 개인 자산 관리의 모든 것.
        </p>
        <button
          type="button"
          onClick={login}
          className="mt-8 rounded-xl bg-accent px-7 py-3.5 text-base font-semibold text-white shadow-sm transition hover:bg-accent-hover"
        >
          토스증권 계정으로 시작하기
        </button>
        <p className="mt-3 text-xs text-muted">Keycloak 보안 로그인 · 토큰은 브라우저에 저장되지 않습니다</p>
      </section>

      <section className="grid gap-5 sm:grid-cols-3">
        {FEATURES.map((f) => (
          <div key={f.title} className="rounded-2xl border border-line bg-surface p-6">
            <div className="text-3xl">{f.emoji}</div>
            <h3 className="mt-3 text-lg font-bold text-ink">{f.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-muted">{f.desc}</p>
          </div>
        ))}
      </section>

      <section className="rounded-2xl border border-line bg-surface p-8">
        <h2 className="text-xl font-bold text-ink">왜 곳간인가</h2>
        <dl className="mt-6 grid gap-6 sm:grid-cols-2">
          {EDGES.map(([k, v]) => (
            <div key={k}>
              <dt className="font-semibold text-accent">{k}</dt>
              <dd className="mt-1 text-sm leading-relaxed text-muted">{v}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="flex flex-col items-center rounded-2xl border border-line bg-[#f6efe5] py-10 text-center">
        <h2 className="text-2xl font-bold text-ink">지금 시작하세요</h2>
        <p className="mt-2 text-sm text-muted">로그인하면 자산·백테스트·시뮬레이터를 바로 사용할 수 있습니다.</p>
        <button
          type="button"
          onClick={login}
          className="mt-6 rounded-xl bg-accent px-7 py-3.5 font-semibold text-white transition hover:bg-accent-hover"
        >
          로그인 / 시작하기
        </button>
      </section>
    </div>
  )
}
