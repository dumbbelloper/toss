package com.toss.service;

import com.toss.client.MarketDataClient;
import com.toss.client.dto.Currency;
import com.toss.client.dto.OrderbookResponse;
import com.toss.client.dto.PriceResponse;
import com.toss.common.TossApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketDataServiceTest {

    private MockRestServiceServer server;
    private MarketDataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss.test");
        server = MockRestServiceServer.bindTo(builder).build();
        MarketDataClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(MarketDataClient.class);
        service = new MarketDataService(client);
    }

    @Test
    void parsesPriceEnvelope() {
        server.expect(requestTo("http://toss.test/api/v1/prices?symbols=005930"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"result":[{"symbol":"005930","timestamp":"2026-03-25T09:30:00.123+09:00",
                        "lastPrice":"72000","currency":"KRW"}]}""", APPLICATION_JSON));

        List<PriceResponse> prices = service.prices("005930");

        assertThat(prices).hasSize(1);
        PriceResponse p = prices.getFirst();
        assertThat(p.symbol()).isEqualTo("005930");
        assertThat(p.lastPrice()).isEqualByComparingTo("72000");
        assertThat(p.currency()).isEqualTo(Currency.KRW);
        server.verify();
    }

    @Test
    void parsesOrderbookEnvelope() {
        server.expect(requestTo("http://toss.test/api/v1/orderbook?symbol=005930"))
                .andRespond(withSuccess("""
                        {"result":{"timestamp":null,"currency":"KRW",
                        "asks":[{"price":"72100","volume":"10"}],
                        "bids":[{"price":"72000","volume":"5"}]}}""", APPLICATION_JSON));

        OrderbookResponse ob = service.orderbook("005930");

        assertThat(ob.currency()).isEqualTo(Currency.KRW);
        assertThat(ob.asks()).hasSize(1);
        assertThat(ob.asks().getFirst().price()).isEqualByComparingTo("72100");
        assertThat(ob.bids().getFirst().volume()).isEqualByComparingTo("5");
        server.verify();
    }

    @Test
    void mapsErrorEnvelopeToTossApiException() {
        server.expect(requestTo("http://toss.test/api/v1/orderbook?symbol=ZZZ"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(APPLICATION_JSON)
                        .body("""
                                {"error":{"requestId":"req-1","code":"stock-not-found",
                                "message":"종목을 찾을 수 없습니다"}}"""));

        assertThatThrownBy(() -> service.orderbook("ZZZ"))
                .asInstanceOf(throwable(TossApiException.class))
                .satisfies(e -> {
                    assertThat(e.code()).isEqualTo("stock-not-found");
                    assertThat(e.requestId()).isEqualTo("req-1");
                    assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
