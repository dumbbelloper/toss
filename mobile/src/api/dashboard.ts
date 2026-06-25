// 토스 대시보드 조회 API (/api/dashboard/*) 의 타입·훅·포맷터.
// web/src/lib/dashboard.ts 를 모바일용으로 미러링한다. 차이: 색은 Tailwind 클래스가 아니라
// RN style 용 hex 문자열을 반환한다.

import { useQuery } from '@tanstack/react-query';

import { ApiError, api } from './client';
import { colors } from '../ui/theme';

export type CurrencyCode = 'KRW' | 'USD';

export interface Price {
  krw: number;
  usd: number | null;
}

export interface HoldingsItem {
  symbol: string;
  name: string;
  marketCountry: string;
  currency: CurrencyCode;
  quantity: number;
  lastPrice: number;
  averagePurchasePrice: number;
  marketValue: { purchaseAmount: number; amount: number; amountAfterCost: number };
  profitLoss: { amount: number; amountAfterCost: number; rate: number; rateAfterCost: number };
  dailyProfitLoss: { amount: number; rate: number };
  cost: { commission: number; tax: number | null };
}

export interface HoldingsOverview {
  totalPurchaseAmount: Price;
  marketValue: { amount: Price; amountAfterCost: Price };
  profitLoss: { amount: Price; amountAfterCost: Price; rate: number; rateAfterCost: number };
  dailyProfitLoss: { amount: Price; rate: number };
  items: HoldingsItem[];
}

export interface Order {
  orderId: string;
  symbol: string;
  side: string;
  orderType: string;
  status: string;
  price: number | null;
  quantity: number;
  currency: CurrencyCode;
  orderedAt: string;
}

const retry = (count: number, err: unknown) =>
  !(err instanceof ApiError && err.status === 401) && count < 1;

/** 보유 주식 + 손익 요약. 계좌(toss.account-seq) 미설정 시 에러를 던질 수 있다. */
export function usePortfolio() {
  return useQuery({
    queryKey: ['dashboard', 'portfolio'],
    queryFn: () => api<HoldingsOverview>('/api/dashboard/portfolio'),
    retry,
    staleTime: 30_000,
  });
}

/** 대기중(미체결) 주문. */
export function useOpenOrders() {
  return useQuery({
    queryKey: ['dashboard', 'orders'],
    queryFn: () => api<Order[]>('/api/dashboard/orders'),
    retry,
    staleTime: 30_000,
  });
}

// ── 포맷 헬퍼 ──────────────────────────────────────────────────────────────

export function formatMoney(amount: number, currency: CurrencyCode): string {
  if (currency === 'USD') {
    return '$' + amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  return Math.round(amount).toLocaleString('ko-KR') + '원';
}

export function formatPrice(price: Price): string {
  const parts: string[] = [];
  if (price.krw) parts.push(formatMoney(price.krw, 'KRW'));
  if (price.usd != null && price.usd !== 0) parts.push(formatMoney(price.usd, 'USD'));
  return parts.length ? parts.join(' · ') : '0원';
}

export function formatPercent(rate: number): string {
  const pct = rate * 100;
  return (pct > 0 ? '+' : '') + pct.toFixed(2) + '%';
}

/** 손익 부호별 색 (한국 관습: 수익=빨강, 손실=파랑). RN style 용 hex. */
export function signColor(value: number): string {
  if (value > 0) return colors.gain;
  if (value < 0) return colors.loss;
  return colors.muted;
}
