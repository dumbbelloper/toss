package com.toss.sim;

/**
 * ETF 세후 월배당 계산 결과 (retiremoni SIM01).
 *
 * @param principal            투자 원금(원)
 * @param yieldPercent         연 배당수익률(%)
 * @param annualGross          세전 연 배당금
 * @param taxRate              적용 세율 (분리과세 0.154 = 배당세 14% + 지방세 1.4%)
 * @param taxAmount            세금
 * @param annualAfterTax       세후 연 배당금
 * @param monthlyAfterTax      세후 월평균 배당금
 * @param afterTaxYield        세후 실효 수익률(%)
 * @param comprehensiveTaxable 금융소득종합과세 대상 여부 (연 금융소득 2,000만원 초과)
 */
public record DividendResult(
        long principal,
        double yieldPercent,
        long annualGross,
        double taxRate,
        long taxAmount,
        long annualAfterTax,
        long monthlyAfterTax,
        double afterTaxYield,
        boolean comprehensiveTaxable
) {
}
