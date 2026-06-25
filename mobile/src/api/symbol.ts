// 국내상장 ETF(.KS/.KQ)는 종목코드(069500.KS)가 비직관적이라 종목명을 노출한다.
// 미국 등 해외 티커(SPY·QQQ)는 직관적이라 그대로 코드를 쓴다. 모든 화면 공통 규칙.

export function isKrListed(symbol: string): boolean {
  return /\.(KS|KQ)$/i.test(symbol);
}

/** 사용자 노출용 라벨. 국내 ETF는 종목명, 그 외는 티커. */
export function symbolLabel(symbol: string, name?: string | null): string {
  return isKrListed(symbol) && name ? name : symbol;
}
