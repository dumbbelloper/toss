package com.toss.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** 유니버스 과거 시세 + 환율을 DB 에 백필한다(배치). */
@Service
public class BackfillService {

    private static final Logger log = LoggerFactory.getLogger(BackfillService.class);

    private final YahooHistoryProvider provider;
    private final PriceDailyDao dao;

    public BackfillService(YahooHistoryProvider provider, PriceDailyDao dao) {
        this.provider = provider;
        this.dao = dao;
    }

    /** 유니버스 전체 백필 + USDKRW. 성공 적재 심볼 수 반환. */
    public int backfillUniverse() {
        List<String> symbols = dao.enabledUniverse();
        int ok = 0;
        for (String symbol : symbols) {
            try {
                List<DailyBar> bars = provider.dailyHistory(symbol);
                if (bars.isEmpty()) {
                    log.warn("백필 빈 응답: {}", symbol);
                    continue;
                }
                int n = dao.upsertPrices(symbol, "US", "yahoo", bars);
                log.info("백필 {}: {}봉 ({} ~ {})", symbol, n, bars.getFirst().date(), bars.getLast().date());
                ok++;
            } catch (Exception e) {
                log.warn("백필 실패 {}: {}", symbol, e.toString());
            }
        }
        backfillFx();
        return ok;
    }

    /** USDKRW 환율 백필. */
    public void backfillFx() {
        try {
            List<FxRate> usdkrw = provider.fxHistory("KRW=X");
            if (!usdkrw.isEmpty()) {
                dao.upsertFx("USDKRW", "yahoo", usdkrw);
                log.info("백필 USDKRW: {}건 ({} ~ {})", usdkrw.size(), usdkrw.getFirst().date(), usdkrw.getLast().date());
            }
        } catch (Exception e) {
            log.warn("환율 백필 실패: {}", e.toString());
        }
    }
}
