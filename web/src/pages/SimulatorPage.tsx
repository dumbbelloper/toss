import { useState, type ReactNode } from 'react'

import { isUnauthorized, login, useMe } from '../lib/auth'
import { useUniverse } from '../lib/backtest'
import { formatMoney } from '../lib/dashboard'
import {
  TAX_CLASS_LABEL,
  useDividend,
  type Contribution,
  type DividendParams,
  type DividendResult,
} from '../lib/sim'
import { LineChart, type ChartSeries } from '../ui/LineChart'

const today = new Date().toISOString().slice(0, 10)

const DEFAULTS: DividendParams = {
  symbol: 'SPY',
  contribution: 'LUMP_SUM',
  amount: 10_000_000,
  start: '2020-01-01',
  end: today,
  reinvest: true,
}

type Mode = 'single' | 'compare'

export function SimulatorPage() {
  const me = useMe()
  const universe = useUniverse().data ?? []
  const [mode, setMode] = useState<Mode>('single')
  const [form, setForm] = useState<DividendParams>(DEFAULTS)
  const [symbolB, setSymbolB] = useState('360750.KS')
  const [submitted, setSubmitted] = useState<{ a: DividendParams; b: DividendParams | null } | null>(null)

  const a = useDividend(submitted?.a ?? null)
  const b = useDividend(submitted?.b ?? null)

  if (me.isLoading) return <p className="text-muted">불러오는 중…</p>
  if (isUnauthorized(me.error)) {
    return (
      <section className="space-y-6">
        <h1 className="text-2xl font-bold">로그인이 필요합니다</h1>
        <button
          type="button"
          onClick={login}
          className="rounded-lg bg-accent px-5 py-3 font-semibold text-white hover:bg-accent-hover"
        >
          Keycloak 으로 로그인
        </button>
      </section>
    )
  }

  const set = <K extends keyof DividendParams>(k: K, v: DividendParams[K]) =>
    setForm((s) => ({ ...s, [k]: v }))
  const run = () =>
    setSubmitted({ a: form, b: mode === 'compare' ? { ...form, symbol: symbolB } : null })
  const amountLabel = form.contribution === 'LUMP_SUM' ? '투자금 (원)' : '월 납입액 (원)'
  const options = universe.length ? universe : [{ symbol: 'SPY', name: 'SPDR S&P 500' }]

  return (
    <section className="space-y-8">
      <header>
        <h1 className="text-2xl font-bold">시뮬레이터 · ETF 세후 배당 현금흐름</h1>
        <p className="mt-1 text-sm text-muted">
          내부 DB의 <b>실제 분배금</b>으로 세후 현금흐름을 계산합니다. 해외 ETF는 그 당시 환율(USDKRW) 적용 ·
          분배금세 국내 15.4% / 해외 현지 15%. 참고용(세무 전문가 확인 전제).
        </p>
      </header>

      <div className="space-y-4 rounded-xl border border-line bg-surface p-5">
        <Segmented
          value={mode}
          onChange={(m) => setMode(m as Mode)}
          options={[
            { value: 'single', label: '단일' },
            { value: 'compare', label: '국내 vs 해외 비교' },
          ]}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={mode === 'compare' ? '종목 A' : '종목'}>
            <Select value={form.symbol} onChange={(v) => set('symbol', v)}>
              {options.map((o) => (
                <option key={o.symbol} value={o.symbol}>
                  {o.name} ({o.symbol})
                </option>
              ))}
            </Select>
          </Field>
          {mode === 'compare' && (
            <Field label="종목 B">
              <Select value={symbolB} onChange={setSymbolB}>
                {options.map((o) => (
                  <option key={o.symbol} value={o.symbol}>
                    {o.name} ({o.symbol})
                  </option>
                ))}
              </Select>
            </Field>
          )}
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="투자 방식">
            <Segmented
              value={form.contribution}
              onChange={(v) => set('contribution', v as Contribution)}
              options={[
                { value: 'LUMP_SUM', label: '일시금 거치' },
                { value: 'MONTHLY', label: '월 적립' },
              ]}
            />
          </Field>
          <Field label="배당">
            <Segmented
              value={form.reinvest ? 'y' : 'n'}
              onChange={(v) => set('reinvest', v === 'y')}
              options={[
                { value: 'y', label: '세후 재투자(DRIP)' },
                { value: 'n', label: '현금 수령' },
              ]}
            />
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label={amountLabel}>
            <NumberInput value={form.amount} step={1_000_000} onChange={(v) => set('amount', v)} />
          </Field>
          <Field label="시작일">
            <DateInput value={form.start} onChange={(v) => set('start', v)} />
          </Field>
          <Field label="종료일">
            <DateInput value={form.end} onChange={(v) => set('end', v)} />
          </Field>
        </div>

        <button
          type="button"
          onClick={run}
          disabled={a.isFetching || b.isFetching}
          className="w-full rounded-lg bg-accent py-3 font-semibold text-white hover:bg-accent-hover disabled:opacity-50"
        >
          {a.isFetching || b.isFetching ? '계산 중…' : '계산'}
        </button>
      </div>

      {(a.isError || b.isError) && (
        <p className="text-sm text-loss">
          계산 실패: {((a.error ?? b.error) as Error | undefined)?.message}
        </p>
      )}

      {submitted && a.data && !submitted.b && <Single r={a.data} />}
      {submitted && submitted.b && a.data && b.data && <Compare a={a.data} b={b.data} />}
    </section>
  )
}

function Single({ r }: { r: DividendResult }) {
  const series: ChartSeries[] = [
    { values: r.timeline.map((p) => p.cumulativeNet), color: '#b5703c', fill: true },
  ]
  return (
    <div className="space-y-5">
      <div className="rounded-xl border border-line bg-surface p-6">
        <div className="flex items-baseline justify-between">
          <span className="text-sm text-muted">세후 누적 분배금{r.reinvest ? ' (재투자됨)' : ''}</span>
          <ClassBadge r={r} />
        </div>
        <div className="mt-1 text-4xl font-extrabold text-accent">
          {formatMoney(Math.round(r.totalNetDividend), 'KRW')}
        </div>
        <div className="mt-1 text-sm text-muted">
          투자원금 대비 연 {(r.yieldOnCost * 100).toFixed(2)}% (yield on cost) · 세전{' '}
          {formatMoney(Math.round(r.totalGrossDividend), 'KRW')}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="투자 원금" value={formatMoney(Math.round(r.invested), 'KRW')} />
        <Stat label="평가액(현재)" value={formatMoney(Math.round(r.finalValue), 'KRW')} />
        <Stat label="보유 주수" value={`${r.finalShares.toFixed(2)}주`} />
      </div>

      <Warnings r={r} />

      <div className="rounded-xl border border-line bg-surface p-4">
        <div className="mb-2 flex items-baseline justify-between">
          <h2 className="font-semibold">누적 세후 분배금</h2>
          <span className="text-xs text-muted">
            {r.name} · {r.timeline.length}회 분배
          </span>
        </div>
        <LineChart
          series={series}
          xLabels={r.timeline.map((p) => p.date)}
          formatY={(v) => Math.round(v / 10000).toLocaleString('ko-KR') + '만'}
        />
      </div>
    </div>
  )
}

function Compare({ a, b }: { a: DividendResult; b: DividendResult }) {
  const series: ChartSeries[] = [
    { values: a.timeline.map((p) => p.cumulativeNet), color: '#b5703c', label: a.symbol },
    { values: b.timeline.map((p) => p.cumulativeNet), color: '#2f6fed', label: b.symbol },
  ]
  const longer = a.timeline.length >= b.timeline.length ? a : b
  const diff = a.totalNetDividend - b.totalNetDividend
  return (
    <div className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <CompareCol r={a} accent />
        <CompareCol r={b} />
      </div>

      <div className="rounded-xl border border-line bg-surface p-4 text-sm">
        <b>{a.symbol}</b> 세후 분배금이 <b>{b.symbol}</b> 대비{' '}
        <span className={diff >= 0 ? 'font-semibold text-gain' : 'font-semibold text-loss'}>
          {diff >= 0 ? '+' : ''}
          {formatMoney(Math.round(diff), 'KRW')}
        </span>{' '}
        ({TAX_CLASS_LABEL[a.taxClass]} vs {TAX_CLASS_LABEL[b.taxClass]}). 동일 노출이라도 과세·환율로 세후가 갈립니다.
      </div>

      <Warnings r={a} prefix={`${a.symbol}: `} />
      <Warnings r={b} prefix={`${b.symbol}: `} />

      <div className="rounded-xl border border-line bg-surface p-4">
        <h2 className="mb-2 font-semibold">누적 세후 분배금 (A vs B)</h2>
        <LineChart
          series={series}
          xLabels={longer.timeline.map((p) => p.date)}
          formatY={(v) => Math.round(v / 10000).toLocaleString('ko-KR') + '만'}
        />
      </div>
    </div>
  )
}

function CompareCol({ r, accent }: { r: DividendResult; accent?: boolean }) {
  return (
    <div className={`rounded-xl border bg-surface p-5 ${accent ? 'border-accent' : 'border-line'}`}>
      <div className="flex items-baseline justify-between">
        <span className="font-semibold">{r.symbol}</span>
        <ClassBadge r={r} />
      </div>
      <div className="mt-2 text-2xl font-extrabold text-accent">
        {formatMoney(Math.round(r.totalNetDividend), 'KRW')}
      </div>
      <dl className="mt-3 space-y-1 text-sm">
        <Line k="세전 분배금" v={formatMoney(Math.round(r.totalGrossDividend), 'KRW')} />
        <Line k="yield on cost" v={`${(r.yieldOnCost * 100).toFixed(2)}%`} />
        <Line k="평가액" v={formatMoney(Math.round(r.finalValue), 'KRW')} />
      </dl>
    </div>
  )
}

function Line({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-muted">{k}</dt>
      <dd className="font-medium">{v}</dd>
    </div>
  )
}

function ClassBadge({ r }: { r: DividendResult }) {
  return (
    <span className="rounded-full bg-line/60 px-2 py-0.5 text-xs text-muted">
      {TAX_CLASS_LABEL[r.taxClass]} · {r.currency}
    </span>
  )
}

function Warnings({ r, prefix = '' }: { r: DividendResult; prefix?: string }) {
  if (!r.healthInsuranceRisk && !r.comprehensiveTaxRisk) return null
  const annual = formatMoney(Math.round(r.maxAnnualGrossDividend), 'KRW')
  return (
    <div className="space-y-2">
      {r.healthInsuranceRisk && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <p className="font-medium">{prefix}건강보험료 영향 가능</p>
          <p className="mt-1 text-amber-700">
            연 배당(최대 {annual})이 금융소득 1,000만원을 초과합니다. 전액이 건보료 부과소득에 반영되고,
            피부양자 자격이 박탈(지역가입자 전환)될 수 있습니다.
          </p>
        </div>
      )}
      {r.comprehensiveTaxRisk && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          <p className="font-medium">{prefix}금융소득종합과세 대상</p>
          <p className="mt-1 text-rose-700">
            연 금융소득이 2,000만원을 초과합니다. 초과분은 다른 소득과 합산해 누진세율(6~45%)이 적용될 수 있어
            실제 세금이 분리과세(15.4%)보다 클 수 있습니다.
          </p>
        </div>
      )}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border border-line bg-surface p-5">
      <span className="text-xs text-muted">{label}</span>
      <span className="text-lg font-bold text-ink">{value}</span>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="text-muted">{label}</span>
      {children}
      <style>{`.siminput{width:100%;border:1px solid var(--color-line);background:#fff;border-radius:8px;padding:8px 10px;font-size:14px;color:var(--color-ink)}.siminput:focus{outline:2px solid var(--color-accent)}`}</style>
    </label>
  )
}

function Segmented({
  value,
  onChange,
  options,
}: {
  value: string
  onChange: (v: string) => void
  options: { value: string; label: string }[]
}) {
  return (
    <div className="inline-flex flex-wrap gap-1 rounded-lg bg-line/40 p-1">
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          onClick={() => onChange(o.value)}
          className={`rounded-md px-3 py-1.5 text-sm font-medium ${
            value === o.value ? 'bg-surface text-ink shadow-sm' : 'text-muted'
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}

function Select({
  value,
  onChange,
  children,
}: {
  value: string
  onChange: (v: string) => void
  children: ReactNode
}) {
  return (
    <select className="siminput" value={value} onChange={(e) => onChange(e.target.value)}>
      {children}
    </select>
  )
}

function NumberInput({
  value,
  onChange,
  step = 1,
}: {
  value: number
  onChange: (v: number) => void
  step?: number
}) {
  return (
    <input
      type="number"
      className="siminput"
      value={value}
      step={step}
      min={0}
      onChange={(e) => onChange(Math.max(0, Number(e.target.value)))}
    />
  )
}

function DateInput({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <input
      type="date"
      className="siminput"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  )
}
