package com.toss.service;

import com.toss.client.OrderClient;
import com.toss.client.dto.Order;
import com.toss.client.dto.PaginatedOrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrderQueryServiceTest {

    private MockRestServiceServer server;
    private OrderQueryService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss.test");
        server = MockRestServiceServer.bindTo(builder).build();
        OrderClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(OrderClient.class);
        service = new OrderQueryService(client);
    }

    @Test
    void openOrdersSendsStatusParam() {
        server.expect(requestTo("http://toss.test/api/v1/orders?status=OPEN"))
                .andRespond(withSuccess("""
                        {"result":{"orders":[{"orderId":"o1","symbol":"005930","side":"BUY","orderType":"LIMIT",
                        "timeInForce":"DAY","status":"PENDING","price":"70000","quantity":"10","orderAmount":null,
                        "currency":"KRW","orderedAt":"2026-03-25T09:30:00+09:00","canceledAt":null,
                        "execution":{"filledQuantity":"0","averageFilledPrice":null,"filledAmount":null,
                        "commission":null,"tax":null,"filledAt":null,"settlementDate":null}}],
                        "nextCursor":null,"hasNext":false}}""", APPLICATION_JSON));

        PaginatedOrderResponse page = service.openOrders();

        assertThat(page.hasNext()).isFalse();
        assertThat(page.orders()).singleElement().satisfies(o -> {
            assertThat(o.orderId()).isEqualTo("o1");
            assertThat(o.status()).isEqualTo("PENDING");
            assertThat(o.price()).isEqualByComparingTo("70000");
            assertThat(o.execution().filledQuantity()).isEqualByComparingTo("0");
        });
        server.verify();
    }

    @Test
    void orderDetailToleratesUnknownStatusCode() {
        // 스펙상 unknown status code 도 허용해야 한다 → String 바인딩으로 역직렬화 성공해야 함
        server.expect(requestTo("http://toss.test/api/v1/orders/o2"))
                .andRespond(withSuccess("""
                        {"result":{"orderId":"o2","symbol":"AAPL","side":"BUY","orderType":"MARKET",
                        "timeInForce":"DAY","status":"SOME_FUTURE_STATUS","price":null,"quantity":"3",
                        "orderAmount":null,"currency":"USD","orderedAt":"2026-03-25T22:30:00+09:00",
                        "canceledAt":null,"execution":{"filledQuantity":"3","averageFilledPrice":"190.5",
                        "filledAmount":"571.5","commission":"0.4","tax":null,"filledAt":"2026-03-25T22:30:01+09:00",
                        "settlementDate":"2026-03-27"}}}""", APPLICATION_JSON));

        Order o = service.order("o2");

        assertThat(o.status()).isEqualTo("SOME_FUTURE_STATUS"); // 알 수 없는 코드도 그대로 보존
        assertThat(o.execution().averageFilledPrice()).isEqualByComparingTo("190.5");
        server.verify();
    }
}
