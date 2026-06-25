package com.toss.digest;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandlePageResponse;
import com.toss.client.dto.Currency;
import com.toss.client.dto.ExchangeRateResponse;
import com.toss.monitor.WatchlistService;
import com.toss.notify.NotificationPort;
import com.toss.service.MarketDataService;
import com.toss.service.MarketInfoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDigestServiceTest {

    private final MarketDataService marketData = mock(MarketDataService.class);
    private final MarketInfoService marketInfo = mock(MarketInfoService.class);
    private final NotificationPort notifications = mock(NotificationPort.class);
    private final WatchlistService watchlist = mock(WatchlistService.class);
    private final MarketDigestService service =
            new MarketDigestService(marketData, marketInfo, notifications, watchlist);

    private static CandlePageResponse twoCandles(String prevClose, String lastClose) {
        return new CandlePageResponse(List.of(
                new Candle("2026-06-22T00:00:00+09:00", new BigDecimal(prevClose), new BigDecimal(prevClose),
                        new BigDecimal(prevClose), new BigDecimal(prevClose), new BigDecimal("1"), Currency.KRW),
                new Candle("2026-06-23T00:00:00+09:00", new BigDecimal(lastClose), new BigDecimal(lastClose),
                        new BigDecimal(lastClose), new BigDecimal(lastClose), new BigDecimal("1"), Currency.KRW)),
                null);
    }

    @Test
    void buildsDigestWithExchangeRateIndicesAndWatchlist() {
        when(marketInfo.exchangeRate(any(), any())).thenReturn(new ExchangeRateResponse(
                Currency.USD, Currency.KRW, new BigDecimal("1536.67"), new BigDecimal("1536.17"), "DOWN"));
        when(marketData.candles(any(), any(), any())).thenReturn(twoCandles("100", "110")); // +10%
        when(watchlist.symbols()).thenReturn(Set.of("005930", "AAPL")); // 관심종목 전체

        String msg = service.buildMessage();

        assertThat(msg)
                .contains("시장 요약")
                .contains("USD/KRW").contains("1,536.67").contains("▼")
                .contains("코스피").contains("코스닥").contains("나스닥").contains("S&P500")
                .contains("관심종목 (2)").contains("005930").contains("AAPL")
                .contains("▲").contains("+10.00%");
    }

    @Test
    void sendDispatchesBuiltMessageToNotificationPort() {
        when(marketInfo.exchangeRate(any(), any())).thenReturn(new ExchangeRateResponse(
                Currency.USD, Currency.KRW, new BigDecimal("1500"), new BigDecimal("1500"), "FLAT"));
        when(marketData.candles(any(), any(), any())).thenReturn(twoCandles("100", "110"));

        String msg = service.sendDigest();

        verify(notifications).send(msg);
        assertThat(msg).isNotBlank();
    }

    @Test
    void toleratesDataFailuresWithoutThrowing() {
        when(marketInfo.exchangeRate(any(), any())).thenThrow(new RuntimeException("boom"));
        when(marketData.candles(any(), any(), any())).thenThrow(new RuntimeException("boom"));

        assertThatCode(service::buildMessage).doesNotThrowAnyException();
        assertThat(service.buildMessage()).contains("시장 요약").contains("환율 조회 실패");
    }
}
