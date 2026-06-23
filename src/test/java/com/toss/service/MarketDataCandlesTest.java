package com.toss.service;

import com.toss.client.MarketDataClient;
import com.toss.client.dto.CandleInterval;
import com.toss.client.dto.CandlePageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketDataCandlesTest {

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
    void candlesSendsParamsAndParses() {
        server.expect(requestTo(startsWith("http://toss.test/api/v1/candles?")))
                .andExpect(queryParam("symbol", "005930"))
                .andExpect(queryParam("interval", "1d"))
                .andExpect(queryParam("count", "2"))
                .andExpect(queryParam("adjusted", "true"))
                .andRespond(withSuccess("""
                        {"result":{"candles":[
                          {"timestamp":"2026-03-24T00:00:00+09:00","openPrice":"71000","highPrice":"72500",
                           "lowPrice":"70800","closePrice":"72000","volume":"1000000","currency":"KRW"},
                          {"timestamp":"2026-03-25T00:00:00+09:00","openPrice":"72000","highPrice":"73000",
                           "lowPrice":"71500","closePrice":"72800","volume":"1200000","currency":"KRW"}],
                        "nextBefore":null}}""", APPLICATION_JSON));

        CandlePageResponse page = service.candles("005930", CandleInterval.DAY, 2);

        assertThat(page.candles()).hasSize(2);
        assertThat(page.candles().getLast().closePrice()).isEqualByComparingTo("72800");
        assertThat(page.nextBefore()).isNull();
        server.verify();
    }
}
