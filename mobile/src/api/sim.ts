// 배당 시뮬레이터 API (/api/sim/dividend). web/src/lib/sim.ts 미러.
// 실분배금 기반 세후 현금흐름(국내/해외 ETF, 일시금·월적립, DRIP, 건보료·종합과세 경고).

import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from './client';

export type Contribution = 'LUMP_SUM' | 'MONTHLY';

export interface DividendParams {
  symbol: string;
  contribution: Contribution;
  amount: number;
  start: string; // YYYY-MM-DD
  end: string;
  reinvest: boolean;
}

export interface DividendPoint {
  date: string;
  netDividend: number;
  cumulativeNet: number;
  value: number;
}

export interface DividendResult {
  symbol: string;
  name: string;
  taxClass: 'KR_EQUITY' | 'KR_OTHER' | 'FOREIGN';
  currency: 'KRW' | 'USD';
  invested: number;
  finalShares: number;
  finalValue: number;
  totalGrossDividend: number;
  totalNetDividend: number;
  yieldOnCost: number;
  reinvest: boolean;
  maxAnnualGrossDividend: number;
  comprehensiveTaxRisk: boolean;
  healthInsuranceRisk: boolean;
  timeline: DividendPoint[];
}

export const TAX_CLASS_LABEL: Record<DividendResult['taxClass'], string> = {
  KR_EQUITY: '국내주식형 ETF',
  KR_OTHER: '국내상장 해외·기타 ETF',
  FOREIGN: '해외상장 ETF',
};

export function useDividend(p: DividendParams | null) {
  return useQuery({
    queryKey: ['sim', 'dividend', p],
    enabled: !!p,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 1,
    queryFn: () => {
      const i = p!;
      const q = new URLSearchParams({
        symbol: i.symbol,
        contribution: i.contribution,
        amount: String(i.amount),
        start: i.start,
        end: i.end,
        reinvest: String(i.reinvest),
      });
      return api<DividendResult>(`/api/sim/dividend?${q.toString()}`);
    },
  });
}
