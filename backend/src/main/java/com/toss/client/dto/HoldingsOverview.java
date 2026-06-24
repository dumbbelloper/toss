package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * 보유 주식 조회 결과 ({@code GET /api/v1/holdings}).
 * 합산 금액은 통화별({@link Price}) 로 분리되며, 손익률은 전체 자산을 원화 환산한 기준.
 *
 * @param totalPurchaseAmount 투자원금 (통화별 합산)
 * @param marketValue         시장 평가금액 (통화별 합산)
 * @param profitLoss          손익 (통화별 합산 + 원화환산 손익률)
 * @param dailyProfitLoss     일간 손익
 * @param items               보유 종목 목록 (없으면 빈 배열)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HoldingsOverview(
        Price totalPurchaseAmount,
        OverviewMarketValue marketValue,
        OverviewProfitLoss profitLoss,
        OverviewDailyProfitLoss dailyProfitLoss,
        List<HoldingsItem> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewMarketValue(Price amount, Price amountAfterCost) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewProfitLoss(
            Price amount,
            Price amountAfterCost,
            BigDecimal rate,
            BigDecimal rateAfterCost
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewDailyProfitLoss(Price amount, BigDecimal rate) {
    }
}
