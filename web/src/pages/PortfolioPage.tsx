import { useState, type ReactNode } from 'react'

import { isUnauthorized, login, useMe } from '../lib/auth'
import { useCandles, useComparison } from '../lib/charts'
import {
  formatMoney,
  formatPercent,
  formatPrice,
  signColor,
  useOpenOrders,
  usePortfolio,
  type HoldingsItem,
} from '../lib/dashboard'
import { LineChart, type ChartSeries } from '../ui/LineChart'

// 다중 시계열 색 팔레트.
const PALETTE = ['#b5703c', '#2f6fed', '#0e7c7b', '#d98b2b', '#8b5cf6', '#ec4899']

export function PortfolioPage() {
  const me = useMe()

  if (me.isLoading) {
    return <p className="text-gray-400">불러오는 중…</p>
  }
  if (isUnauthorized(me.error)) {
    return <LoginPrompt />
  }

  return (
    <section className="space-y-8">
      <h1 className="text-2xl font-bold">자산 관리</h1>
      <PortfolioSummary />
      <PriceChart />
      <Holdings />
      <PerformanceChart />
      <OpenOrders />
    </section>
  )
}

/** 시세 차트. 보유 종목 중 선택(없으면 샘플 005930). 계좌 없이도 동작. */
function PriceChart() {
  const portfolio = usePortfolio()
  const holdings = portfolio.data?.items ?? []
  const symbols = holdings.length
    ? holdings.map((h) => ({ symbol: h.symbol, name: h.name || h.symbol }))
    : [{ symbol: '005930', name: '삼성전자' }]

  const [symbol, setSymbol] = useState(symbols[0].symbol)
  const active = symbols.find((s) => s.symbol === symbol) ?? symbols[0]
  const { data, isLoading, isError } = useCandles(active.symbol)

  const closes = data?.candles.map((c) => c.closePrice) ?? []
  const series: ChartSeries[] = [{ values: closes, color: '#b5703c', fill: true }]

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">시세 · {active.name}</h2>
        {symbols.length > 1 && (
          <select
            value={symbol}
            onChange={(e) => setSymbol(e.target.value)}
            className="rounded-md border border-gray-300 bg-white px-2 py-1 text-sm"
          >
            {symbols.map((s) => (
              <option key={s.symbol} value={s.symbol}>
                {s.name}
              </option>
            ))}
          </select>
        )}
      </div>
      <div className="rounded-xl border border-gray-200 bg-white p-4">
        {isLoading ? (
          <div className="h-[220px] animate-pulse rounded bg-gray-50" />
        ) : isError || closes.length < 2 ? (
          <p className="py-12 text-center text-sm text-gray-400">시세를 불러올 수 없습니다.</p>
        ) : (
          <LineChart
            series={series}
            xLabels={data?.candles.map((c) => c.timestamp) ?? []}
            formatY={(v) => Math.round(v).toLocaleString('ko-KR')}
          />
        )}
      </div>
    </div>
  )
}

/** 보유 종목 매수가 대비 성과 비교(100=손익분기). 계좌 설정 시 표시. */
function PerformanceChart() {
  const { data, isLoading, isError } = useComparison()

  if (isLoading || isError || !data || !data.length) return null

  const series: ChartSeries[] = data
    .filter((s) => s.points.length > 1)
    .map((s, i) => ({
      values: s.points.map((p) => p.value),
      color: PALETTE[i % PALETTE.length],
      label: s.name || s.symbol,
    }))
  if (!series.length) return null
  const xLabels = data.find((s) => s.points.length > 1)?.points.map((p) => p.time) ?? []

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">매수가 대비 성과</h2>
      <div className="rounded-xl border border-gray-200 bg-white p-4">
        <LineChart series={series} baseline={100} xLabels={xLabels} formatY={(v) => v.toFixed(0)} />
      </div>
    </div>
  )
}

function LoginPrompt() {
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

function PortfolioSummary() {
  const { data, isLoading, isError } = usePortfolio()

  if (isLoading) return <SkeletonCards />
  if (isError || !data) return <AccountNotice />

  const pl = data.profitLoss
  const daily = data.dailyProfitLoss
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <Card label="평가금액">
        <span className="text-xl font-bold">{formatPrice(data.marketValue.amount)}</span>
      </Card>
      <Card label="총 손익">
        <span className={`text-xl font-bold ${signColor(pl.rate)}`}>
          {formatPrice(pl.amount)}
        </span>
        <span className={`text-sm font-medium ${signColor(pl.rate)}`}>{formatPercent(pl.rate)}</span>
      </Card>
      <Card label="일간 손익">
        <span className={`text-xl font-bold ${signColor(daily.rate)}`}>
          {formatPrice(daily.amount)}
        </span>
        <span className={`text-sm font-medium ${signColor(daily.rate)}`}>
          {formatPercent(daily.rate)}
        </span>
      </Card>
    </div>
  )
}

function Holdings() {
  const { data, isLoading, isError } = usePortfolio()

  if (isLoading || isError || !data) return null
  if (!data.items.length) {
    return <p className="text-sm text-gray-400">보유 종목이 없습니다.</p>
  }

  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100 text-left text-xs text-gray-400">
            <th className="px-4 py-3 font-medium">종목</th>
            <th className="px-4 py-3 text-right font-medium">수량</th>
            <th className="px-4 py-3 text-right font-medium">평가금액</th>
            <th className="px-4 py-3 text-right font-medium">손익</th>
          </tr>
        </thead>
        <tbody>
          {data.items.map((item) => (
            <HoldingRow key={item.symbol} item={item} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function HoldingRow({ item }: { item: HoldingsItem }) {
  const c = item.currency
  return (
    <tr className="border-b border-gray-50 last:border-0">
      <td className="px-4 py-3">
        <div className="font-medium">{item.name || item.symbol}</div>
        <div className="text-xs text-gray-400">{item.symbol}</div>
      </td>
      <td className="px-4 py-3 text-right tabular-nums">{item.quantity.toLocaleString('ko-KR')}</td>
      <td className="px-4 py-3 text-right tabular-nums">
        {formatMoney(item.marketValue.amount, c)}
      </td>
      <td className={`px-4 py-3 text-right tabular-nums ${signColor(item.profitLoss.rate)}`}>
        <div>{formatMoney(item.profitLoss.amount, c)}</div>
        <div className="text-xs">{formatPercent(item.profitLoss.rate)}</div>
      </td>
    </tr>
  )
}

function OpenOrders() {
  const { data, isLoading, isError } = useOpenOrders()

  if (isLoading || isError || !data || !data.length) return null

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold">대기 주문</h2>
      <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100 text-left text-xs text-gray-400">
              <th className="px-4 py-3 font-medium">종목</th>
              <th className="px-4 py-3 font-medium">구분</th>
              <th className="px-4 py-3 text-right font-medium">수량</th>
              <th className="px-4 py-3 text-right font-medium">가격</th>
              <th className="px-4 py-3 font-medium">상태</th>
            </tr>
          </thead>
          <tbody>
            {data.map((o) => (
              <tr key={o.orderId} className="border-b border-gray-50 last:border-0">
                <td className="px-4 py-3 font-medium">{o.symbol}</td>
                <td className={`px-4 py-3 ${o.side === 'BUY' ? 'text-red-600' : 'text-blue-600'}`}>
                  {o.side === 'BUY' ? '매수' : o.side === 'SELL' ? '매도' : o.side}
                </td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {o.quantity.toLocaleString('ko-KR')}
                </td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {o.price != null ? formatMoney(o.price, o.currency) : '시장가'}
                </td>
                <td className="px-4 py-3 text-gray-500">{o.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function Card({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1 rounded-xl border border-gray-200 bg-white p-5">
      <span className="text-xs text-gray-400">{label}</span>
      {children}
    </div>
  )
}

function SkeletonCards() {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {[0, 1, 2].map((i) => (
        <div key={i} className="h-24 animate-pulse rounded-xl border border-gray-200 bg-white" />
      ))}
    </div>
  )
}

/** 포트폴리오 조회 실패 — 보통 toss.account-seq 미설정. */
function AccountNotice() {
  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
      <p className="font-medium">포트폴리오를 불러오지 못했습니다.</p>
      <p className="mt-1 text-amber-700">
        토스 계좌(<code className="font-mono">toss.account-seq</code>)가 설정돼 있어야 보유·주문이 조회됩니다.
        시세/캔들 기능은 계좌 없이도 동작합니다.
      </p>
    </div>
  )
}
