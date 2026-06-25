// 차트 데이터 훅 (/api/dashboard/candles, /api/dashboard/comparison).
// 백엔드 DTO(com.toss.client.dto.Candle / com.toss.dashboard.ComparisonSeries) 미러.

import { useQuery } from '@tanstack/react-query'

import { ApiError, api } from './api'
import type { CurrencyCode } from './dashboard'

export interface Candle {
  timestamp: string
  openPrice: number
  highPrice: number
  lowPrice: number
  closePrice: number
  volume: number
  currency: CurrencyCode
}

export interface CandlePageResponse {
  candles: Candle[]
  nextBefore: string | null
}

export type CandleInterval = 'MINUTE' | 'DAY' | 'WEEK' | 'MONTH'

/** 보유 종목별 "매수가 대비 성과" 시계열. value 는 100 기준(100=손익분기). */
export interface ComparisonSeries {
  symbol: string
  name: string
  averagePurchasePrice: number
  purchaseDate: string | null
  points: { time: string; value: number }[]
}

const retry = (count: number, err: unknown) =>
  !(err instanceof ApiError && err.status === 401) && count < 1

/** 캔들(OHLCV). 시세는 계좌 없이 토큰만으로 동작한다. */
export function useCandles(symbol: string, interval: CandleInterval = 'DAY', count = 120) {
  return useQuery({
    queryKey: ['dashboard', 'candles', symbol, interval, count],
    queryFn: () =>
      api<CandlePageResponse>(
        `/api/dashboard/candles?symbol=${encodeURIComponent(symbol)}&interval=${interval}&count=${count}`,
      ),
    enabled: !!symbol,
    retry,
    staleTime: 60_000,
  })
}

/** 보유 종목 매수가 대비 성과 비교. 계좌(toss.account-seq) 필요. */
export function useComparison(count = 120) {
  return useQuery({
    queryKey: ['dashboard', 'comparison', count],
    queryFn: () => api<ComparisonSeries[]>(`/api/dashboard/comparison?count=${count}`),
    retry,
    staleTime: 60_000,
  })
}
