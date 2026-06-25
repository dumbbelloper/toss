package com.toss.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DividendSimServiceTest {

    private final DividendSimService service = new DividendSimService();

    @Test
    void computesAfterTaxMonthlyDividend() {
        // 1억 × 8% = 800만 세전 → 세금 800만×15.4% = 1,232,000 → 세후 6,768,000 → 월 564,000
        DividendResult r = service.dividend(100_000_000L, 8.0, 0);
        assertThat(r.annualGross()).isEqualTo(8_000_000L);
        assertThat(r.taxAmount()).isEqualTo(1_232_000L);
        assertThat(r.annualAfterTax()).isEqualTo(6_768_000L);
        assertThat(r.monthlyAfterTax()).isEqualTo(564_000L);
        assertThat(r.comprehensiveTaxable()).isFalse();
    }

    @Test
    void flagsComprehensiveTaxationOverThreshold() {
        // 3억 × 8% = 2,400만 > 2,000만 → 금융소득종합과세 대상
        assertThat(service.dividend(300_000_000L, 8.0, 0).comprehensiveTaxable()).isTrue();
    }

    @Test
    void rejectsNegativeInput() {
        assertThatThrownBy(() -> service.dividend(-1, 8, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
