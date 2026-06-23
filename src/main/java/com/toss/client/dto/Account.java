package com.toss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 계좌 ({@code GET /api/v1/accounts}).
 *
 * @param accountNo   계좌번호
 * @param accountSeq  계좌 식별 키. 주문 등 API 호출 시 {@code X-Tossinvest-Account} 로 사용
 * @param accountType 계좌 유형 (현재 BROKERAGE 만 지원)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(
        String accountNo,
        long accountSeq,
        String accountType
) {
}
