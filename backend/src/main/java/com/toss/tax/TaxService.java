package com.toss.tax;

import org.springframework.stereotype.Service;

/**
 * 한국 ETF 세제 계산(원화 기준). 규칙·임계값 단일출처: docs/product/korea-tax.md (2026-06, 교차검증 완료).
 * 참고용 — 실제 절세 의사결정은 세무 전문가 확인 전제.
 */
@Service
public class TaxService {

    public static final double DIVIDEND_RATE = 0.154;             // 배당소득세 14% + 지방 1.4%
    public static final double US_DIVIDEND_WITHHOLDING = 0.15;    // 해외 배당 현지 원천(외국납부세액공제로 추가 ≈ 0)
    public static final double CAPITAL_GAINS_RATE = 0.22;         // 해외 양도세 20% + 지방 2%
    public static final double FOREIGN_GAIN_DEDUCTION = 2_500_000;// 연 250만(국내+국외 합산)
    public static final double KR_OTHER_GAIN_RATE = 0.154;        // 국내상장 기타 ETF 매매차익 = 배당소득
    public static final double COMPREHENSIVE_TAX_THRESHOLD = 20_000_000; // 금융소득종합과세 2,000만
    public static final double HEALTH_INSURANCE_THRESHOLD = 10_000_000;  // 건보료 트리거 1,000만

    /** 분배금 세후(세전 원화 금액 입력). */
    public double netDividend(double gross, TaxClass tc) {
        double rate = (tc == TaxClass.FOREIGN) ? US_DIVIDEND_WITHHOLDING : DIVIDEND_RATE;
        return gross * (1 - rate);
    }

    /** 분배금 세금액. */
    public double dividendTax(double gross, TaxClass tc) {
        return gross - netDividend(gross, tc);
    }

    /**
     * 매매차익 세후. {@code foreignGainBefore} = 같은 해 이미 실현한 해외 양도차익(250만 공제 한계계산용).
     * KR_OTHER 는 과표기준가 미보유로 실차익 근사(실제는 min[실차익, 과표기준가증분] × 15.4%, 보수적 상한).
     */
    public double netCapitalGain(double gain, TaxClass tc, double foreignGainBefore) {
        return switch (tc) {
            case KR_EQUITY -> gain;
            case KR_OTHER -> gain - Math.max(0, gain) * KR_OTHER_GAIN_RATE;
            case FOREIGN -> {
                double taxable = Math.max(0, foreignGainBefore + gain - FOREIGN_GAIN_DEDUCTION)
                        - Math.max(0, foreignGainBefore - FOREIGN_GAIN_DEDUCTION);
                yield gain - taxable * CAPITAL_GAINS_RATE;
            }
        };
    }

    /**
     * 연 금융소득(이자+배당 + kr_other 매매차익; 해외 양도차익은 분류과세라 제외)에 대한 경고 판정.
     */
    public FinancialIncomeFlags assess(double annualFinancialIncome) {
        return new FinancialIncomeFlags(
                annualFinancialIncome,
                annualFinancialIncome > COMPREHENSIVE_TAX_THRESHOLD,
                annualFinancialIncome > HEALTH_INSURANCE_THRESHOLD);
    }

    /** 금융소득 임계값 판정 결과. */
    public record FinancialIncomeFlags(double financialIncome, boolean comprehensiveTax, boolean healthInsuranceRisk) {
    }
}
