package com.toss.history;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

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

    public List<String> enabledUniverse() {
        return jdbc.getJdbcTemplate().queryForList(
                "select symbol from symbol_universe where enabled = true order by symbol", String.class);
    }

    /** 적재 현황(심볼별 봉 수·기간). */
    public List<Map<String, Object>> coverage() {
        return jdbc.getJdbcTemplate().queryForList(
                "select symbol, count(*) as bars, min(date) as from_date, max(date) as to_date "
                        + "from price_daily group by symbol order by symbol");
    }
}
