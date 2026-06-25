// 백테스트 API (/api/backtest). web/src/lib/backtest.ts 미러.

import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from './client';

export type Strategy = 'BUY_AND_HOLD' | 'SMA_CROSS' | 'RSI';

export const STRATEGIES: { value: Strategy; label: string }[] = [
  { value: 'BUY_AND_HOLD', label: '매수후보유' },
  { value: 'SMA_CROSS', label: 'SMA 교차' },
  { value: 'RSI', label: 'RSI' },
];

export interface BacktestParams {
  symbol: string;
  strategy: Strategy;
  shortWindow: number;
  longWindow: number;
  rsiPeriod: number;
  rsiBuyBelow: number;
  rsiSellAbove: number;
  count: number;
  capital: number;
}

export interface BacktestResult {
  symbol: string;
  strategy: string;
  params: string;
  bars: number;
  initialCapital: number;
  finalEquity: number;
  totalReturn: number;
  buyHoldReturn: number;
  maxDrawdown: number;
  trades: number;
  winRate: number;
  equity: { time: string; value: number }[];
}

export function useBacktest(params: BacktestParams | null) {
  return useQuery({
    queryKey: ['backtest', params],
    enabled: !!params,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 1,
    queryFn: () => {
      const p = params!;
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
      });
      return api<BacktestResult>(`/api/backtest?${q.toString()}`);
    },
  });
}
