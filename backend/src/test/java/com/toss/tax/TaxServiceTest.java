package com.toss.tax;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TaxServiceTest {

    private final TaxService tax = new TaxService();

    @Test
    void dividendNetByClass() {
        // 국내 ETF: 15.4%, 해외 ETF: 현지 15%
        assertThat(tax.netDividend(1000, TaxClass.KR_EQUITY)).isCloseTo(846.0, within(1e-9));
        assertThat(tax.netDividend(1000, TaxClass.KR_OTHER)).isCloseTo(846.0, within(1e-9));
        assertThat(tax.netDividend(1000, TaxClass.FOREIGN)).isCloseTo(850.0, within(1e-9));
    }

    @Test
    void krEquityCapitalGainTaxFree() {
        assertThat(tax.netCapitalGain(10_000_000, TaxClass.KR_EQUITY, 0)).isCloseTo(10_000_000, within(1e-6));
    }

    @Test
    void krOtherCapitalGainTaxedAsDividend() {
        // 1,000만 차익 → 15.4% 과세 → 846만
        assertThat(tax.netCapitalGain(10_000_000, TaxClass.KR_OTHER, 0)).isCloseTo(8_460_000, within(1e-6));
    }

    @Test
    void foreignCapitalGainUsesAnnualDeduction() {
        // 250만 이하 → 비과세
        assertThat(tax.netCapitalGain(2_000_000, TaxClass.FOREIGN, 0)).isCloseTo(2_000_000, within(1e-6));
        // 1,000만 차익 → (1000-250)만 × 22% = 165만 세금 → 835만
        assertThat(tax.netCapitalGain(10_000_000, TaxClass.FOREIGN, 0)).isCloseTo(8_350_000, within(1e-6));
    }

    @Test
    void foreignDeductionConsumedAcrossSales() {
        // 이미 250만 공제 소진 후 추가 1,000만 → 전액 과세 220만 → 780만
        assertThat(tax.netCapitalGain(10_000_000, TaxClass.FOREIGN, 2_500_000)).isCloseTo(7_800_000, within(1e-6));
    }

    @Test
    void financialIncomeThresholds() {
        assertThat(tax.assess(9_000_000).healthInsuranceRisk()).isFalse();
        assertThat(tax.assess(12_000_000).healthInsuranceRisk()).isTrue();
        assertThat(tax.assess(12_000_000).comprehensiveTax()).isFalse();
        assertThat(tax.assess(25_000_000).comprehensiveTax()).isTrue();
    }
}
