// 은퇴 시뮬레이터 API (/api/sim/*). web/src/lib/sim.ts 미러.

import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from './client';

export interface DividendInput {
  principal: number;
  yieldPercent: number;
  otherFinancialIncome: number;
}

export interface DividendResult {
  principal: number;
  yieldPercent: number;
  annualGross: number;
  taxRate: number;
  taxAmount: number;
  annualAfterTax: number;
  monthlyAfterTax: number;
  afterTaxYield: number;
  comprehensiveTaxable: boolean;
}

/** SIM01: ETF 세후 월배당. 입력이 바뀌면 실시간 재계산. */
export function useDividend(input: DividendInput | null) {
  return useQuery({
    queryKey: ['sim', 'dividend', input],
    enabled: !!input && input.principal > 0,
    retry: (count, err) => !(err instanceof ApiError && err.status === 401) && count < 1,
    queryFn: () => {
      const i = input!;
      const q = new URLSearchParams({
        principal: String(i.principal),
        yieldPercent: String(i.yieldPercent),
        otherFinancialIncome: String(i.otherFinancialIncome),
      });
      return api<DividendResult>(`/api/sim/dividend?${q.toString()}`);
    },
  });
}
