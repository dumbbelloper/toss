package com.toss.sim;

import com.toss.history.DividendRow;
import com.toss.history.PriceDailyDao;
import com.toss.history.PriceRow;
import com.toss.tax.TaxService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DividendSimServiceTest {

    private final PriceDailyDao dao = mock(PriceDailyDao.class);
    private final DividendSimService sim = new DividendSimService(dao, new TaxService());

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private DividendSimService.Params lump(String symbol, boolean reinvest) {
        return new DividendSimService.Params(symbol, DividendSimService.Contribution.LUMP_SUM,
                10_000_000, d("2022-01-01"), d("2022-12-31"), reinvest);
    }

    @Test
    void krEtfLumpSumNetDividend() {
        when(dao.symbolMeta("K")).thenReturn(new PriceDailyDao.SymbolMeta("KODEX", "KRW", "kr_other"));
        when(dao.priceRange(eq("K"), any(), any())).thenReturn(List.of(
                new PriceRow(d("2022-01-03"), 10000, 10000),
                new PriceRow(d("2022-12-30"), 12000, 12000)));
        when(dao.dividendsRange(eq("K"), any(), any())).thenReturn(List.of(
                new DividendRow(d("2022-06-30"), 300))); // 주당 300원

        var r = sim.run(lump("K", false));

        // 10,000,000 / 10,000 = 1,000주. 배당 1,000×300 = 300,000 세전, 세후 ×0.846 = 253,800
        assertThat(r.finalShares()).isCloseTo(1000, within(1e-6));
        assertThat(r.totalGrossDividend()).isCloseTo(300_000, within(1e-3));
        assertThat(r.totalNetDividend()).isCloseTo(253_800, within(1e-3));
        assertThat(r.finalValue()).isCloseTo(12_000_000, within(1e-3)); // 1000주 × 12,000
    }

    @Test
    void foreignAppliesFxAndUsWithholding() {
        when(dao.symbolMeta("SPY")).thenReturn(new PriceDailyDao.SymbolMeta("SPY", "USD", "foreign"));
        when(dao.priceRange(eq("SPY"), any(), any())).thenReturn(List.of(
                new PriceRow(d("2022-01-03"), 400, 400),
                new PriceRow(d("2022-12-30"), 440, 440)));
        when(dao.dividendsRange(eq("SPY"), any(), any())).thenReturn(List.of(
                new DividendRow(d("2022-06-30"), 1.5))); // $1.5/주
        var fx = new TreeMap<LocalDate, Double>();
        fx.put(d("2022-01-03"), 1200.0);
        fx.put(d("2022-06-30"), 1300.0);
        when(dao.fxSeries(eq("USDKRW"), any())).thenReturn(fx);

        var r = sim.run(new DividendSimService.Params("SPY", DividendSimService.Contribution.LUMP_SUM,
                12_000_000, d("2022-01-01"), d("2022-12-31"), false));

        // 매수가 400×1200 = 480,000원/주 → 12,000,000/480,000 = 25주
        // 배당 25 × 1.5 × 1300 = 48,750 세전, 세후 ×0.85 = 41,437.5 (미국 원천 15%)
        assertThat(r.finalShares()).isCloseTo(25, within(1e-6));
        assertThat(r.totalGrossDividend()).isCloseTo(48_750, within(1e-2));
        assertThat(r.totalNetDividend()).isCloseTo(41_437.5, within(1e-2));
    }

    @Test
    void reinvestGrowsShares() {
        when(dao.symbolMeta("K")).thenReturn(new PriceDailyDao.SymbolMeta("KODEX200", "KRW", "kr_equity"));
        when(dao.priceRange(eq("K"), any(), any())).thenReturn(List.of(
                new PriceRow(d("2022-01-03"), 10000, 10000),
                new PriceRow(d("2022-12-30"), 10000, 10000)));
        when(dao.dividendsRange(eq("K"), any(), any())).thenReturn(List.of(
                new DividendRow(d("2022-06-30"), 1000))); // 주당 1,000원

        var r = sim.run(lump("K", true));

        // 1,000주. 분배금 1,000×1,000 = 1,000,000 세전, 분배금세 15.4% → 세후 846,000.
        // 재투자 846,000/10,000 = 84.6주 → 1,084.6주
        assertThat(r.finalShares()).isCloseTo(1084.6, within(1e-3));
        assertThat(r.totalNetDividend()).isCloseTo(846_000, within(1e-3));
    }

    @Test
    void flagsHealthInsuranceAndComprehensiveByAnnualGross() {
        when(dao.symbolMeta("K")).thenReturn(new PriceDailyDao.SymbolMeta("HI", "KRW", "kr_other"));
        when(dao.priceRange(eq("K"), any(), any())).thenReturn(List.of(
                new PriceRow(d("2022-01-03"), 10000, 10000),
                new PriceRow(d("2022-12-30"), 10000, 10000)));
        // 1,000주 × 15,000원 = 15,000,000 세전 배당 → 건보료(>1000만) O, 종합과세(>2000만) X
        when(dao.dividendsRange(eq("K"), any(), any())).thenReturn(List.of(
                new DividendRow(d("2022-06-30"), 15000)));

        var r = sim.run(lump("K", false));

        assertThat(r.maxAnnualGrossDividend()).isCloseTo(15_000_000, within(1e-3));
        assertThat(r.healthInsuranceRisk()).isTrue();
        assertThat(r.comprehensiveTaxRisk()).isFalse();
    }
}
