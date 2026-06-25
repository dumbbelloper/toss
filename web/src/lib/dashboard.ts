// 토스 대시보드 조회 API (/api/dashboard/*) 의 타입과 훅.
// 백엔드 DTO(com.toss.client.dto.*) 미러. BigDecimal 은 JSON number 로 직렬화된다.

import { useQuery } from '@tanstack/react-query'

import { ApiError, api } from './api'

export type CurrencyCode = 'KRW' | 'USD'

/** 통화별 합산 금액. krw 는 국내 종목 없으면 0, usd 는 해외 종목 없으면 null. */
export interface Price {
  krw: number
  usd: number | null
}

export interface HoldingsItem {
  symbol: string
  name: string
  marketCountry: string
  currency: CurrencyCode
  quantity: number
  lastPrice: number
  averagePurchasePrice: number
  marketValue: { purchaseAmount: number; amount: number; amountAfterCost: number }
  profitLoss: { amount: number; amountAfterCost: number; rate: number; rateAfterCost: number }
  dailyProfitLoss: { amount: number; rate: number }
  cost: { commission: number; tax: number | null }
}

export interface HoldingsOverview {
  totalPurchaseAmount: Price
  marketValue: { amount: Price; amountAfterCost: Price }
  profitLoss: { amount: Price; amountAfterCost: Price; rate: number; rateAfterCost: number }
  dailyProfitLoss: { amount: Price; rate: number }
  items: HoldingsItem[]
}

export interface Order {
  orderId: string
  symbol: string
  side: string
  orderType: string
  timeInForce: string
  status: string
  price: number | null
  quantity: number
  orderAmount: number | null
  currency: CurrencyCode
  orderedAt: string
  canceledAt: string | null
}

// 401(미인증) 은 정상 결과이므로 재시도하지 않는다. 그 외(예: 계좌 미설정 500)는 1회만 재시도.
const retry = (count: number, err: unknown) =>
  !(err instanceof ApiError && err.status === 401) && count < 1

/** 보유 주식 + 손익 요약. 계좌(toss.account-seq) 미설정 시 에러를 던질 수 있다. */
export function usePortfolio() {
  return useQuery({
    queryKey: ['dashboard', 'portfolio'],
    queryFn: () => api<HoldingsOverview>('/api/dashboard/portfolio'),
    retry,
    staleTime: 30_000,
  })
}

/** 대기중(미체결) 주문. */
export function useOpenOrders() {
  return useQuery({
    queryKey: ['dashboard', 'orders'],
    queryFn: () => api<Order[]>('/api/dashboard/orders'),
    retry,
    staleTime: 30_000,
  })
}

// ── 포맷 헬퍼 ──────────────────────────────────────────────────────────────

export function formatMoney(amount: number, currency: CurrencyCode): string {
  if (currency === 'USD') {
    return '$' + amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return Math.round(amount).toLocaleString('ko-KR') + '원'
}

/** 통화별 합산 금액(Price)을 사람이 읽는 문자열로. 둘 다 있으면 줄바꿈 없이 ' · ' 로. */
export function formatPrice(price: Price): string {
  const parts: string[] = []
  if (price.krw) parts.push(formatMoney(price.krw, 'KRW'))
  if (price.usd != null && price.usd !== 0) parts.push(formatMoney(price.usd, 'USD'))
  return parts.length ? parts.join(' · ') : '0원'
}

/** rate 는 소수비율(0.1077 = 10.77%). 부호 포함. */
export function formatPercent(rate: number): string {
  const pct = rate * 100
  return (pct > 0 ? '+' : '') + pct.toFixed(2) + '%'
}

/**
 * 손익 부호별 색 클래스 (한국 관습: 수익=빨강, 손실=파랑).
 * 0 이면 회색.
 */
export function signColor(value: number): string {
  if (value > 0) return 'text-red-600'
  if (value < 0) return 'text-blue-600'
  return 'text-gray-500'
}
