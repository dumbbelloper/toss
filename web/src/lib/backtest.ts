// 백테스트 API (/api/backtest). 백엔드 com.toss.backtest.* 미러.

import { useQuery } from '@tanstack/react-query'

import { ApiError, api } from './api'

export type Strategy = 'BUY_AND_HOLD' | 'SMA_CROSS' | 'RSI'

export const STRATEGIES: { value: Strategy; label: string }[] = [
  { value: 'BUY_AND_HOLD', label: '매수 후 보유 (벤치마크)' },
  { value: 'SMA_CROSS', label: '이동평균 교차 (SMA)' },
  { value: 'RSI', label: 'RSI 과매도·과매수' },
]

export type Currency = 'USD' | 'KRW'

export interface BacktestParams {
  symbol: string
  strategy: Strategy
  shortWindow: number
  longWindow: number
  rsiPeriod: number
  rsiBuyBelow: number
  rsiSellAbove: number
  count: number
  capital: number
  /** 배당 재투자(총수익) vs 가격수익. */
  reinvest: boolean
  /** USD 원본 vs 그 당시 환율 적용 KRW. */
  currency: Currency
}

export interface UniverseItem {
  symbol: string
  name: string
  category: string
  bars: number
  from_date: string | null
  to_date: string | null
}

/** 백테스트 종목 선택용 유니버스(+커버리지). */
export function useUniverse() {
  return useQuery({
    queryKey: ['history', 'universe'],
    queryFn: () => api<UniverseItem[]>('/api/history/universe'),
    staleTime: 300_000,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 1,
  })
}

export interface BacktestResult {
  symbol: string
  strategy: string
  params: string
  bars: number
  initialCapital: number
  finalEquity: number
  /** 전략 총수익률 (소수비율, 0.21 = +21%). */
  totalReturn: number
  /** 같은 구간 매수후보유 수익률(벤치마크). */
  buyHoldReturn: number
  /** 최대 낙폭(MDD, 양수 비율). */
  maxDrawdown: number
  trades: number
  /** 승률 (소수비율). */
  winRate: number
  /** 자본 곡선. */
  equity: { time: string; value: number }[]
}

/** params 가 있으면 실행. 미설정(null)이면 호출 안 함. */
export function useBacktest(params: BacktestParams | null) {
  return useQuery({
    queryKey: ['backtest', params],
    enabled: !!params,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 1,
    queryFn: () => {
      const p = params!
      const q = new URLSearchParams({
        symbol: p.symbol,
        strategy: p.strategy,
        shortWindow: String(p.shortWindow),
        longWindow: String(p.longWindow),
        rsiPeriod: String(p.rsiPeriod),
        rsiBuyBelow: String(p.rsiBuyBelow),
        rsiSellAbove: String(p.rsiSellAbove),
        count: String(p.count),
        capital: String(p.capital),
        reinvest: String(p.reinvest),
        currency: p.currency,
      })
      return api<BacktestResult>(`/api/backtest?${q.toString()}`)
    },
  })
}
