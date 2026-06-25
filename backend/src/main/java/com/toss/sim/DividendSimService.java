package com.toss.sim;

import com.toss.history.DividendRow;
import com.toss.history.PriceDailyDao;
import com.toss.history.PriceRow;
import com.toss.tax.TaxClass;
import com.toss.tax.TaxService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 배당 시뮬레이터(SIM01). 내부 DB의 실분배금·가격·환율 + TaxService 로 세후 현금흐름을 계산한다.
 * 전부 원화 기준(해외 ETF 는 그 당시 USDKRW 적용). 규칙: docs/product/korea-tax.md
 */
@Service
public class DividendSimService {

    public enum Contribution { LUMP_SUM, MONTHLY }

    public record Params(String symbol, Contribution contribution, double amount,
                         LocalDate start, LocalDate end, boolean reinvest) {
    }

    /** 분배 시점 한 점(원화). */
    public record Point(String date, double netDividend, double cumulativeNet, double value) {
    }

    public record Result(
            String symbol, String name, String taxClass, String currency,
            double invested, double finalShares, double finalValue,
            double totalGrossDividend, double totalNetDividend, double yieldOnCost,
            boolean reinvest, double maxAnnualGrossDividend,
            boolean comprehensiveTaxRisk, boolean healthInsuranceRisk,
            List<Point> timeline) {
    }

    private record Event(LocalDate date, boolean contribution, double divPerShare) {
    }

    private final PriceDailyDao dao;
    private final TaxService tax;

    public DividendSimService(PriceDailyDao dao, TaxService tax) {
        this.dao = dao;
        this.tax = tax;
    }

    public Result run(Params p) {
        PriceDailyDao.SymbolMeta meta = dao.symbolMeta(p.symbol());
        TaxClass tc = TaxClass.of(meta.taxClass());
        boolean foreign = "USD".equalsIgnoreCase(meta.currency());

        List<PriceRow> prices = dao.priceRange(p.symbol(), p.start(), p.end());
        if (prices.size() < 2) {
            throw new IllegalArgumentException("기간 내 시세 데이터가 부족합니다: " + p.symbol());
        }
        NavigableMap<LocalDate, Double> fx = foreign ? dao.fxSeries("USDKRW", p.end()) : null;

        // 원화 종가 맵(해외는 그 당시 환율 적용)
        NavigableMap<LocalDate, Double> priceKrw = new TreeMap<>();
        for (PriceRow r : prices) {
            priceKrw.put(r.date(), foreign ? r.close() * fxFloor(fx, r.date()) : r.close());
        }

        // 매수(적립) + 배당 이벤트 병합 → 날짜순
        List<Event> events = new ArrayList<>();
        LocalDate first = prices.getFirst().date();
        if (p.contribution() == Contribution.LUMP_SUM) {
            events.add(new Event(first, true, 0));
        } else {
            for (LocalDate d = first; !d.isAfter(p.end()); d = d.plusMonths(1)) {
                events.add(new Event(d, true, 0));
            }
        }
        for (DividendRow d : dao.dividendsRange(p.symbol(), p.start(), p.end())) {
            double perShareKrw = foreign ? d.amount() * fxFloor(fx, d.exDate()) : d.amount();
            events.add(new Event(d.exDate(), false, perShareKrw));
        }
        events.sort((a, b) -> a.date().compareTo(b.date()));

        double shares = 0, invested = 0, grossTotal = 0, netTotal = 0, cumNet = 0;
        NavigableMap<Integer, Double> annualGross = new TreeMap<>();
        List<Point> timeline = new ArrayList<>();

        for (Event e : events) {
            Double px = priceAsOf(priceKrw, e.date());
            if (px == null || px <= 0) {
                continue;
            }
            if (e.contribution()) {
                shares += p.amount() / px;
                invested += p.amount();
            } else {
                double gross = shares * e.divPerShare();
                double net = tax.netDividend(gross, tc);
                grossTotal += gross;
                netTotal += net;
                annualGross.merge(e.date().getYear(), gross, Double::sum);
                if (p.reinvest()) {
                    shares += net / px;
                } else {
                    cumNet += net;
                }
                timeline.add(new Point(e.date().toString(), net, p.reinvest() ? netTotal : cumNet, shares * px));
            }
        }

        double finalValue = shares * priceKrw.lastEntry().getValue();
        double years = Math.max(0.1, (p.end().toEpochDay() - first.toEpochDay()) / 365.25);
        double yieldOnCost = invested > 0 ? (netTotal / years) / invested : 0;
        double maxAnnual = annualGross.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        TaxService.FinancialIncomeFlags flags = tax.assess(maxAnnual);

        return new Result(p.symbol(), meta.name(), tc.name(), meta.currency(),
                invested, shares, finalValue, grossTotal, netTotal, yieldOnCost,
                p.reinvest(), maxAnnual, flags.comprehensiveTax(), flags.healthInsuranceRisk(), timeline);
    }

    private static double fxFloor(NavigableMap<LocalDate, Double> fx, LocalDate d) {
        var e = fx.floorEntry(d);
        if (e == null) {
            e = fx.firstEntry();
        }
        return e != null ? e.getValue() : 1300.0;
    }

    private static Double priceAsOf(NavigableMap<LocalDate, Double> m, LocalDate d) {
        var e = m.floorEntry(d);
        if (e == null) {
            e = m.ceilingEntry(d);
        }
        return e != null ? e.getValue() : null;
    }
}
