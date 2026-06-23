package com.toss.digest;

import com.toss.client.dto.Candle;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.Currency;
import com.toss.client.dto.ExchangeRateResponse;
import com.toss.monitor.WatchlistService;
import com.toss.notify.NotificationPort;
import com.toss.service.MarketDataService;
import com.toss.service.MarketInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 시장 요약을 만들어 알림 채널(텔레그램)로 발송한다.
 * <p>지수는 토스에 직접 조회가 없어 ETF 프록시(KODEX200·코스닥150·QQQ·IVV)를 사용한다.
 * 등락률은 일봉 2개(전일·당일 종가)로 계산한다. 종목별 조회 실패는 건너뛴다.
 */
@Service
public class MarketDigestService {

    private static final Logger log = LoggerFactory.getLogger(MarketDigestService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    /** 지수 프록시: {라벨, 심볼}. */
    private static final List<String[]> INDEX_PROXIES = List.of(
            new String[]{"코스피  (KODEX200)", "069500"},
            new String[]{"코스닥  (코스닥150)", "229200"},
            new String[]{"나스닥  (QQQ)", "QQQ"},
            new String[]{"S&P500 (IVV)", "IVV"});

    private final MarketDataService marketData;
    private final MarketInfoService marketInfo;
    private final NotificationPort notifications;
    private final WatchlistService watchlist;

    public MarketDigestService(MarketDataService marketData, MarketInfoService marketInfo,
                               NotificationPort notifications, WatchlistService watchlist) {
        this.marketData = marketData;
        this.marketInfo = marketInfo;
        this.notifications = notifications;
        this.watchlist = watchlist;
    }

    /** 요약을 만들어 발송하고, 발송한 메시지를 반환한다. */
    public String sendDigest() {
        String message = buildMessage();
        notifications.send(message);
        return message;
    }

    /** 요약 메시지 텍스트 생성 (발송하지 않음). */
    public String buildMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 시장 요약  ").append(LocalDateTime.now(KST).format(TS)).append("\n\n");

        sb.append("💱 환율\n");
        try {
            ExchangeRateResponse er = marketInfo.exchangeRate(Currency.USD, Currency.KRW);
            sb.append("  USD/KRW  ").append(num(er.rate(), 2)).append("원  ")
                    .append(exchangeArrow(er.rateChangeType())).append('\n');
        } catch (Exception e) {
            sb.append("  (환율 조회 실패)\n");
        }
        sb.append('\n');

        sb.append("📈 지수 (ETF 프록시)\n");
        for (String[] proxy : INDEX_PROXIES) {
            sb.append(line(proxy[0], quote(proxy[1])));
        }

        List<String> symbols = watchlist.symbols().stream().sorted().toList();
        if (!symbols.isEmpty()) {
            sb.append("\n⭐ 관심종목 (").append(symbols.size()).append(")\n");
            for (String symbol : symbols) {
                sb.append(line(symbol, quote(symbol)));
            }
        }
        return sb.toString();
    }

    /** 일봉 2개로 현재가·등락률을 구한다. 실패 시 빈 Quote. */
    private Quote quote(String symbol) {
        try {
            List<Candle> cs = marketData.candles(symbol, CandleInterval.DAY, 2).candles().stream()
                    .filter(c -> c.closePrice() != null && c.timestamp() != null)
                    .sorted(Comparator.comparing(Candle::timestamp))
                    .toList();
            if (cs.isEmpty()) {
                return Quote.empty();
            }
            Candle last = cs.getLast();
            Double changePct = null;
            if (cs.size() >= 2) {
                BigDecimal prev = cs.get(cs.size() - 2).closePrice();
                if (prev.signum() != 0) {
                    changePct = last.closePrice().subtract(prev)
                            .divide(prev, 6, RoundingMode.HALF_UP).doubleValue() * 100.0;
                }
            }
            return new Quote(last.closePrice(), last.currency(), changePct);
        } catch (Exception e) {
            log.debug("디제스트 시세 조회 실패 {}: {}", symbol, e.toString());
            return Quote.empty();
        }
    }

    private static String line(String label, Quote q) {
        return "  " + label + "  " + money(q.price(), q.currency()) + "  " + change(q.changePct()) + '\n';
    }

    private static String money(BigDecimal price, Currency cur) {
        if (price == null) {
            return "-";
        }
        return cur == Currency.USD ? "$" + num(price, 2) : num(price, 0) + "원";
    }

    private static String num(BigDecimal v, int decimals) {
        return String.format("%,." + decimals + "f", v);
    }

    private static String change(Double pct) {
        if (pct == null) {
            return "";
        }
        String arrow = pct > 0 ? "▲" : pct < 0 ? "▼" : "•";
        return arrow + " " + String.format("%+.2f%%", pct);
    }

    private static String exchangeArrow(String rateChangeType) {
        if (rateChangeType == null) {
            return "";
        }
        return switch (rateChangeType) {
            case "UP" -> "▲";
            case "DOWN" -> "▼";
            default -> "•";
        };
    }

    private record Quote(BigDecimal price, Currency currency, Double changePct) {
        static Quote empty() {
            return new Quote(null, null, null);
        }
    }
}
