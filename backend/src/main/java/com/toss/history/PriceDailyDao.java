package com.toss.history;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** price_daily / fx_daily 적재·조회. 백테스트가 읽는 단일 출처. */
@Repository
public class PriceDailyDao {

    private final NamedParameterJdbcTemplate jdbc;

    public PriceDailyDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String UPSERT_PRICE = """
            insert into price_daily (symbol, market, date, open, high, low, close, adj_close, volume, source, updated_at)
            values (:symbol, :market, :date, :open, :high, :low, :close, :adj, :volume, :source, now())
            on conflict (symbol, date) do update set
                open = excluded.open, high = excluded.high, low = excluded.low,
                close = excluded.close, adj_close = excluded.adj_close, volume = excluded.volume,
                source = excluded.source, updated_at = now()
            """;

    public int upsertPrices(String symbol, String market, String source, List<DailyBar> bars) {
        if (bars.isEmpty()) {
            return 0;
        }
        SqlParameterSource[] batch = bars.stream().map(b -> new MapSqlParameterSource()
                .addValue("symbol", symbol).addValue("market", market).addValue("date", b.date())
                .addValue("open", b.open()).addValue("high", b.high()).addValue("low", b.low())
                .addValue("close", b.close()).addValue("adj", b.adjClose()).addValue("volume", b.volume())
                .addValue("source", source)).toArray(SqlParameterSource[]::new);
        return jdbc.batchUpdate(UPSERT_PRICE, batch).length;
    }

    private static final String UPSERT_FX = """
            insert into fx_daily (pair, date, rate, source) values (:pair, :date, :rate, :source)
            on conflict (pair, date) do update set rate = excluded.rate, source = excluded.source
            """;

    public int upsertFx(String pair, String source, List<FxRate> rates) {
        if (rates.isEmpty()) {
            return 0;
        }
        SqlParameterSource[] batch = rates.stream().map(x -> new MapSqlParameterSource()
                .addValue("pair", pair).addValue("date", x.date()).addValue("rate", x.rate())
                .addValue("source", source)).toArray(SqlParameterSource[]::new);
        return jdbc.batchUpdate(UPSERT_FX, batch).length;
    }

    private static final String UPSERT_DIV = """
            insert into dividend_daily (symbol, ex_date, amount, currency, source)
            values (:symbol, :exDate, :amount, :currency, :source)
            on conflict (symbol, ex_date) do update set
                amount = excluded.amount, currency = excluded.currency, source = excluded.source
            """;

    public int upsertDividends(String symbol, String currency, String source, List<DividendRow> divs) {
        if (divs.isEmpty()) {
            return 0;
        }
        SqlParameterSource[] batch = divs.stream().map(d -> new MapSqlParameterSource()
                .addValue("symbol", symbol).addValue("exDate", d.exDate())
                .addValue("amount", d.amount()).addValue("currency", currency)
                .addValue("source", source)).toArray(SqlParameterSource[]::new);
        return jdbc.batchUpdate(UPSERT_DIV, batch).length;
    }

    public List<String> enabledUniverse() {
        return jdbc.getJdbcTemplate().queryForList(
                "select symbol from symbol_universe where enabled = true order by symbol", String.class);
    }

    /** 백필용: 심볼 + 마켓 + 통화(상장 통화). */
    public record UniverseSymbol(String symbol, String market, String currency) {
    }

    public List<UniverseSymbol> enabledUniverseDetailed() {
        return jdbc.getJdbcTemplate().query(
                "select symbol, market, currency from symbol_universe where enabled = true order by symbol",
                (rs, i) -> new UniverseSymbol(rs.getString("symbol"), rs.getString("market"), rs.getString("currency")));
    }

    /** 백테스트용: 최근 count 봉(과거→현재 정렬). */
    public List<PriceRow> series(String symbol, int count) {
        List<PriceRow> desc = jdbc.getJdbcTemplate().query(
                "select date, close, adj_close from price_daily where symbol = ? order by date desc limit ?",
                (rs, i) -> new PriceRow(rs.getObject("date", LocalDate.class),
                        rs.getDouble("close"), rs.getDouble("adj_close")),
                symbol, count);
        return desc.reversed();
    }

    /** 시뮬레이터용: 기간 내 일별 가격(과거→현재). */
    public List<PriceRow> priceRange(String symbol, LocalDate from, LocalDate to) {
        return jdbc.getJdbcTemplate().query(
                "select date, close, adj_close from price_daily where symbol = ? and date between ? and ? order by date",
                (rs, i) -> new PriceRow(rs.getObject("date", LocalDate.class),
                        rs.getDouble("close"), rs.getDouble("adj_close")),
                symbol, from, to);
    }

    /** 시뮬레이터용: 기간 내 실분배금(과거→현재). */
    public List<DividendRow> dividendsRange(String symbol, LocalDate from, LocalDate to) {
        return jdbc.getJdbcTemplate().query(
                "select ex_date, amount from dividend_daily where symbol = ? and ex_date between ? and ? order by ex_date",
                (rs, i) -> new DividendRow(rs.getObject("ex_date", LocalDate.class), rs.getDouble("amount")),
                symbol, from, to);
    }

    /** 종목 메타(이름·상장통화·세금분류). */
    public record SymbolMeta(String name, String currency, String taxClass) {
    }

    public SymbolMeta symbolMeta(String symbol) {
        return jdbc.getJdbcTemplate().queryForObject(
                "select name, currency, tax_class from symbol_universe where symbol = ?",
                (rs, i) -> new SymbolMeta(rs.getString("name"), rs.getString("currency"), rs.getString("tax_class")),
                symbol);
    }

    /** 환율 시계열(forward-fill 용 floorEntry 조회). pair 예: 'USDKRW'. to 이하 전부. */
    public NavigableMap<LocalDate, Double> fxSeries(String pair, LocalDate to) {
        NavigableMap<LocalDate, Double> map = new TreeMap<>();
        jdbc.getJdbcTemplate().query(
                "select date, rate from fx_daily where pair = ? and date <= ? order by date",
                (RowCallbackHandler) rs -> map.put(rs.getObject("date", LocalDate.class), rs.getDouble("rate")),
                pair, to);
        return map;
    }

    /** 백테스트 종목 선택용 유니버스(+커버리지). */
    public List<Map<String, Object>> universe() {
        return jdbc.getJdbcTemplate().queryForList(
                "select u.symbol, u.name, u.category, count(p.date) as bars, "
                        + "min(p.date) as from_date, max(p.date) as to_date "
                        + "from symbol_universe u left join price_daily p on p.symbol = u.symbol "
                        + "where u.enabled = true group by u.symbol, u.name, u.category order by u.symbol");
    }

    /** 적재 현황(심볼별 봉 수·기간). */
    public List<Map<String, Object>> coverage() {
        return jdbc.getJdbcTemplate().queryForList(
                "select symbol, count(*) as bars, min(date) as from_date, max(date) as to_date "
                        + "from price_daily group by symbol order by symbol");
    }
}
