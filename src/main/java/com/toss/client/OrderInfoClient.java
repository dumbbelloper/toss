package com.toss.client;

import com.toss.client.dto.BuyingPowerResponse;
import com.toss.client.dto.Commission;
import com.toss.client.dto.Currency;
import com.toss.client.dto.SellableQuantityResponse;
import com.toss.client.dto.TossEnvelope;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 거래 가능 정보 조회 API ({@code X-Tossinvest-Account} 헤더 필요 — 인터셉터가 주입).
 */
@HttpExchange(url = "/api/v1", accept = "application/json")
public interface OrderInfoClient {

    /** 매수 가능 금액 조회 (현금 기반). */
    @GetExchange("/buying-power")
    TossEnvelope<BuyingPowerResponse> getBuyingPower(@RequestParam("currency") Currency currency);

    /** 판매 가능 수량 조회. */
    @GetExchange("/sellable-quantity")
    TossEnvelope<SellableQuantityResponse> getSellableQuantity(@RequestParam("symbol") String symbol);

    /** 매매 수수료 조회 (KR·US 시장별). */
    @GetExchange("/commissions")
    TossEnvelope<List<Commission>> getCommissions();
}
