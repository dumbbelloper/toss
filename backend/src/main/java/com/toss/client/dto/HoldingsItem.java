package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 보유 종목 한 건. 금액 필드는 모두 거래 통화({@link #currency()}) 기준.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HoldingsItem(
        String symbol,
        String name,
        MarketCountry marketCountry,
        Currency currency,
        BigDecimal quantity,
        BigDecimal lastPrice,
        BigDecimal averagePurchasePrice,
        MarketValue marketValue,
        ProfitLoss profitLoss,
        DailyProfitLoss dailyProfitLoss,
        Cost cost
) {

    /** 시장 평가. 거래 통화 기준. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarketValue(
            BigDecimal purchaseAmount,
            BigDecimal amount,
            BigDecimal amountAfterCost
    ) {
    }

    /** 손익. rate 는 소수비율 (0.1077 = 10.77%). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfitLoss(
            BigDecimal amount,
            BigDecimal amountAfterCost,
            BigDecimal rate,
            BigDecimal rateAfterCost
    ) {
    }

    /** 일간 손익. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyProfitLoss(
            BigDecimal amount,
            BigDecimal rate
    ) {
    }

    /** 비용. tax 는 없으면 null. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cost(
            BigDecimal commission,
            BigDecimal tax
    ) {
    }
}
