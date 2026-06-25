package com.toss.sim;

import com.toss.sim.AccountSimService.Params;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AccountSimServiceTest {

    private final AccountSimService svc = new AccountSimService();

    @Test
    void pensionCreditAndWithdrawTax() {
        // 100만 1년, r=10%, 배당0, 65세, 일반소득(공제 13.2%)
        var r = svc.compare(new Params(1_000_000, 0, 1, 0.10, 0, false, 65));

        assertThat(r.general().finalAfterTax()).isCloseTo(1_100_000, within(1.0)); // 양도차익 250만 이하 비과세
        assertThat(r.isa().finalAfterTax()).isCloseTo(1_100_000, within(1.0));     // 100만 이익 < 비과세 한도
        // 연금: 1,100,000×(1-0.055) + 1,000,000×0.132 = 1,039,500 + 132,000 = 1,171,500
        assertThat(r.pension().taxBenefit()).isCloseTo(132_000, within(1.0));
        assertThat(r.pension().tax()).isCloseTo(60_500, within(1.0));
        assertThat(r.pension().finalAfterTax()).isCloseTo(1_171_500, within(1.0));
    }

    @Test
    void dividendDragMakesGeneralWorse() {
        var r = svc.compare(new Params(0, 9_000_000, 10, 0.06, 0.02, false, 65));

        // ISA·연금은 full r → 동일 pretax. 일반은 배당세 드래그로 낮음.
        assertThat(r.isa().finalPretax()).isCloseTo(r.pension().finalPretax(), within(1.0));
        assertThat(r.general().finalPretax()).isLessThan(r.isa().finalPretax());
        // 연금 세액공제 환급 = 9,000,000 × 0.132 × 10
        assertThat(r.pension().taxBenefit()).isCloseTo(9_000_000 * 0.132 * 10, within(1.0));
        assertThat(r.isa().taxBenefit()).isZero();
        assertThat(r.general().timeline()).hasSize(10);
    }

    @Test
    void lowIncomeHigherCreditAndExempt() {
        var hi = svc.compare(new Params(0, 9_000_000, 10, 0.06, 0.02, true, 65));
        var std = svc.compare(new Params(0, 9_000_000, 10, 0.06, 0.02, false, 65));

        // 서민형/저소득: 세액공제 16.5% > 13.2%
        assertThat(hi.pension().taxBenefit()).isGreaterThan(std.pension().taxBenefit());
        // ISA 비과세 한도 400만 > 200만 → 세금 더 적거나 같음
        assertThat(hi.isa().tax()).isLessThanOrEqualTo(std.isa().tax());
    }
}
