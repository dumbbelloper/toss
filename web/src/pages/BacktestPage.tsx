import { useState, type ReactNode } from 'react'

import { isUnauthorized, login, useMe } from '../lib/auth'
import {
  STRATEGIES,
  useBacktest,
  useUniverse,
  type BacktestParams,
  type Currency,
  type Strategy,
} from '../lib/backtest'
import { formatMoney, formatPercent, signColor } from '../lib/dashboard'
import { LineChart, type ChartSeries } from '../ui/LineChart'

const DEFAULTS: BacktestParams = {
  symbol: 'SPY',
  strategy: 'BUY_AND_HOLD',
  shortWindow: 5,
  longWindow: 20,
  rsiPeriod: 14,
  rsiBuyBelow: 30,
  rsiSellAbove: 70,
  count: 1260,
  capital: 1_000_000,
  reinvest: true,
  currency: 'USD',
}

export function BacktestPage() {
  const me = useMe()
  const universe = useUniverse().data ?? []
  const [form, setForm] = useState<BacktestParams>(DEFAULTS)
  const [submitted, setSubmitted] = useState<BacktestParams | null>(null)
  const { data, isFetching, isError, error } = useBacktest(submitted)

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

  const set = <K extends keyof BacktestParams>(k: K, v: BacktestParams[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  return (
    <section className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">백테스팅</h1>
        <p className="mt-1 text-sm text-muted">
          내부 DB의 수정주가(상장일~현재)로 전략을 검증합니다. 예측이 아니라 회고입니다.
        </p>
      </div>

      <form
        className="grid gap-4 rounded-xl border border-line bg-surface p-5 sm:grid-cols-2"
        onSubmit={(e) => {
          e.preventDefault()
          setSubmitted({ ...form })
        }}
      >
        <Field label="종목">
          <select
            value={form.symbol}
            onChange={(e) => set('symbol', e.target.value)}
            className="input"
          >
            {(universe.length ? universe : [{ symbol: form.symbol, name: '' }]).map((u) => (
              <option key={u.symbol} value={u.symbol}>
                {u.symbol}
                {'name' in u && u.name ? ` · ${u.name}` : ''}
              </option>
            ))}
          </select>
        </Field>
        <Field label="배당">
          <select
            value={form.reinvest ? 'y' : 'n'}
            onChange={(e) => set('reinvest', e.target.value === 'y')}
            className="input"
          >
            <option value="y">배당 재투자 (총수익)</option>
            <option value="n">가격 수익 (배당 제외)</option>
          </select>
        </Field>
        <Field label="통화">
          <select
            value={form.currency}
            onChange={(e) => set('currency', e.target.value as Currency)}
            className="input"
          >
            <option value="USD">USD (달러 기준)</option>
            <option value="KRW">KRW (당시 환율 적용)</option>
          </select>
        </Field>
        <Field label="전략">
          <select
            value={form.strategy}
            onChange={(e) => set('strategy', e.target.value as Strategy)}
            className="input"
          >
            {STRATEGIES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </Field>

        {form.strategy === 'SMA_CROSS' && (
          <>
            <Field label="단기 이동평균">
              <NumberInput value={form.shortWindow} onChange={(v) => set('shortWindow', v)} />
            </Field>
            <Field label="장기 이동평균">
              <NumberInput value={form.longWindow} onChange={(v) => set('longWindow', v)} />
            </Field>
          </>
        )}
        {form.strategy === 'RSI' && (
          <>
            <Field label="RSI 기간">
              <NumberInput value={form.rsiPeriod} onChange={(v) => set('rsiPeriod', v)} />
            </Field>
            <Field label="매수 (RSI <)">
              <NumberInput value={form.rsiBuyBelow} onChange={(v) => set('rsiBuyBelow', v)} />
            </Field>
            <Field label="매도 (RSI >)">
              <NumberInput value={form.rsiSellAbove} onChange={(v) => set('rsiSellAbove', v)} />
            </Field>
          </>
        )}

        <Field label="기간">
          <select
            value={form.count}
            onChange={(e) => set('count', Number(e.target.value))}
            className="input"
          >
            <option value={252}>최근 1년</option>
            <option value={756}>최근 3년</option>
            <option value={1260}>최근 5년</option>
            <option value={2520}>최근 10년</option>
            <option value={100000}>전체 (상장일~현재)</option>
          </select>
        </Field>
        <Field label="초기 자본">
          <NumberInput value={form.capital} onChange={(v) => set('capital', v)} step={100_000} />
        </Field>

        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={isFetching}
            className="w-full rounded-lg bg-accent px-5 py-3 font-semibold text-white hover:bg-accent-hover disabled:opacity-50"
          >
            {isFetching ? '백테스트 중…' : '백테스트 실행'}
          </button>
        </div>
      </form>

      {isError && (
        <p className="rounded-lg border border-line bg-surface p-4 text-sm text-loss">
          실행 실패: {(error as Error).message}
        </p>
      )}

      {data && submitted && <Results data={data} currency={submitted.currency} />}

      <style>{`
        .input { width:100%; border:1px solid var(--color-line); background:#fff; border-radius:8px;
          padding:8px 10px; font-size:14px; color:var(--color-ink); }
        .input:focus { outline:2px solid var(--color-accent); outline-offset:0; }
      `}</style>
    </section>
  )
}

function Results({
  data,
  currency,
}: {
  data: import('../lib/backtest').BacktestResult
  currency: Currency
}) {
  const series: ChartSeries[] = [
    { values: data.equity.map((e) => e.value), color: '#b5703c', fill: true },
  ]
  const excess = data.totalReturn - data.buyHoldReturn
  return (
    <div className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-4">
        <Card label="전략 수익률">
          <span className={`text-xl font-bold ${signColor(data.totalReturn)}`}>
            {formatPercent(data.totalReturn)}
          </span>
        </Card>
        <Card label="매수후보유 대비">
          <span className={`text-xl font-bold ${signColor(excess)}`}>{formatPercent(excess)}</span>
          <span className="text-xs text-muted">B&H {formatPercent(data.buyHoldReturn)}</span>
        </Card>
        <Card label="최대 낙폭 (MDD)">
          <span className="text-xl font-bold text-loss">-{(data.maxDrawdown * 100).toFixed(2)}%</span>
        </Card>
        <Card label="매매 · 승률">
          <span className="text-xl font-bold text-ink">{data.trades}회</span>
          <span className="text-xs text-muted">승률 {(data.winRate * 100).toFixed(0)}%</span>
        </Card>
      </div>

      <div className="space-y-2 rounded-xl border border-line bg-surface p-4">
        <div className="flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">자본 곡선</h2>
          <span className="text-sm text-muted">
            {formatMoney(data.initialCapital, currency)} →{' '}
            {formatMoney(Math.round(data.finalEquity), currency)}
          </span>
        </div>
        <LineChart
          series={series}
          baseline={data.initialCapital}
          formatY={(v) =>
            currency === 'KRW'
              ? Math.round(v / 10000).toLocaleString('ko-KR') + '만'
              : '$' + Math.round(v).toLocaleString('en-US')
          }
        />
        <p className="text-xs text-muted">
          {data.symbol} · {data.params} · {data.bars}봉
        </p>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="text-muted">{label}</span>
      {children}
    </label>
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
      value={value}
      step={step}
      onChange={(e) => onChange(Number(e.target.value))}
      className="input"
    />
  )
}

function Card({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border border-line bg-surface p-5">
      <span className="text-xs text-muted">{label}</span>
      {children}
    </div>
  )
}
