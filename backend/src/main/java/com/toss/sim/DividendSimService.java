package com.toss.sim;

import org.springframework.stereotype.Service;

/**
 * ETF 세후 월배당 계산 (retiremoni SIM01).
 *
 * <p>배당소득은 기본 <b>분리과세 15.4%</b>(배당소득세 14% + 지방소득세 1.4%)로 원천징수된다.
 * 단, 연 금융소득(이자+배당)이 <b>2,000만원</b>을 넘으면 <b>금융소득종합과세</b> 대상이 되어
 * 다른 소득과 합산해 누진세율(6~45%)이 적용될 수 있다(이 경우 플래그만 표시 — 실제 세액은
 * 종합소득 전체로 계산해야 하므로 여기선 분리과세 기준값 + 경고를 준다).
 */
@Service
public class DividendSimService {

    /** 배당소득세 14% + 지방소득세 1.4%. */
    private static final double DIVIDEND_TAX_RATE = 0.154;
    /** 금융소득종합과세 기준 (연). */
    private static final long COMPREHENSIVE_THRESHOLD = 20_000_000L;

    public DividendResult dividend(long principal, double yieldPercent, long otherFinancialIncome) {
        if (principal < 0 || yieldPercent < 0 || otherFinancialIncome < 0) {
            throw new IllegalArgumentException("원금·수익률·기타소득은 음수일 수 없습니다.");
        }
        double annualGross = principal * yieldPercent / 100.0;
        long gross = Math.round(annualGross);
        long tax = Math.round(annualGross * DIVIDEND_TAX_RATE);
        long afterTax = gross - tax;
        long monthly = Math.round(afterTax / 12.0);
        double afterTaxYield = principal > 0 ? (double) afterTax / principal * 100.0 : 0.0;
        boolean comprehensive = (gross + otherFinancialIncome) > COMPREHENSIVE_THRESHOLD;

        return new DividendResult(
                principal, yieldPercent, gross, DIVIDEND_TAX_RATE, tax, afterTax, monthly,
                afterTaxYield, comprehensive);
    }
}
