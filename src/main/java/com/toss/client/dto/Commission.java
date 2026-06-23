package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * 매매 수수료 ({@code GET /api/v1/commissions}).
 *
 * @param marketCountry  시장 국가
 * @param commissionRate 수수료율(%). 예: 0.015 = 0.015%
 * @param startDate      적용 시작일 (YYYY-MM-DD, KST). 해외주식은 null
 * @param endDate        적용 종료일 (YYYY-MM-DD, KST). 무기한이면 null
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Commission(
        MarketCountry marketCountry,
        BigDecimal commissionRate,
        String startDate,
        String endDate
) {
}
