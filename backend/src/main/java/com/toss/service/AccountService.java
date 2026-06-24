package com.toss.service;

import com.toss.client.AccountClient;
import com.toss.client.dto.Account;
import com.toss.client.dto.HoldingsOverview;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.function.Supplier;

/**
 * 계좌·자산 조회 서비스. 읽기 전용이므로 일시 에러(429/5xx)에 재시도한다.
 */
@Service
@Retryable(
        includes = TossTransientException.class,
        maxRetriesString = "${toss.retry.max-retries:3}",
        delayString = "${toss.retry.delay-ms:1000}",
        multiplierString = "${toss.retry.multiplier:2.0}",
        maxDelayString = "${toss.retry.max-delay-ms:8000}",
        jitterString = "${toss.retry.jitter-ms:250}")
public class AccountService {

    private final AccountClient client;

    public AccountService(AccountClient client) {
        this.client = client;
    }

    /** 계좌 목록 조회. */
    public List<Account> accounts() {
        return execute(() -> client.getAccounts().result());
    }

    /** 전체 보유 주식 조회. */
    public HoldingsOverview holdings() {
        return execute(() -> client.getHoldings(null).result());
    }

    /** 특정 종목 보유 현황 조회. */
    public HoldingsOverview holdings(String symbol) {
        return execute(() -> client.getHoldings(symbol).result());
    }

    private static <T> T execute(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw TossApiException.from(e);
        }
    }
}
