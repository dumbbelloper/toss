package com.toss.client;

import com.toss.client.dto.Currency;
import com.toss.client.dto.ExchangeRateResponse;
import com.toss.client.dto.TossEnvelope;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 시장 정보 API (환율 등). 토큰만으로 호출 가능.
 */
@HttpExchange(url = "/api/v1", accept = "application/json")
public interface MarketInfoClient {

    /** 환율 조회. */
    @GetExchange("/exchange-rate")
    TossEnvelope<ExchangeRateResponse> getExchangeRate(@RequestParam("baseCurrency") Currency baseCurrency,
                                                       @RequestParam("quoteCurrency") Currency quoteCurrency);
}
