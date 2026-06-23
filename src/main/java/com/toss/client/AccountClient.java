package com.toss.client;

import com.toss.client.dto.Account;
import com.toss.client.dto.HoldingsOverview;
import com.toss.client.dto.TossEnvelope;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 계좌·자산 조회 API 클라이언트.
 * <p>{@code getHoldings} 는 {@code X-Tossinvest-Account} 헤더가 필요하며,
 * 헤더는 {@code AccountHeaderInterceptor} 가 자동 주입한다. {@code getAccounts} 는 불필요.
 */
@HttpExchange(url = "/api/v1", accept = "application/json")
public interface AccountClient {

    /** 계좌 목록 조회. */
    @GetExchange("/accounts")
    TossEnvelope<List<Account>> getAccounts();

    /** 보유 주식 조회. symbol 미지정 시 전체. */
    @GetExchange("/holdings")
    TossEnvelope<HoldingsOverview> getHoldings(@RequestParam(name = "symbol", required = false) String symbol);
}
