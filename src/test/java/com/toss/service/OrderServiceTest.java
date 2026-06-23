package com.toss.service;

import com.toss.client.OrderClient;
import com.toss.client.dto.OrderCreateRequest;
import com.toss.client.dto.Side;
import com.toss.common.TossApiException;
import com.toss.common.TossTransientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrderServiceTest {

    private MockRestServiceServer server;
    private OrderService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://toss.test");
        server = MockRestServiceServer.bindTo(builder).build();
        OrderClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(OrderClient.class);
        service = new OrderService(client);
    }

    @Test
    void marketOrderAutoAssignsIdempotencyKeyAndOmitsPrice() {
        server.expect(requestTo("http://toss.test/api/v1/orders"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.symbol").value("005930"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.orderType").value("MARKET"))
                .andExpect(jsonPath("$.quantity").value("10"))
                .andExpect(jsonPath("$.clientOrderId").exists())   // 자동 멱등성 키
                .andExpect(jsonPath("$.price").doesNotExist())     // NON_NULL 로 생략
                .andExpect(jsonPath("$.orderAmount").doesNotExist())
                .andRespond(withSuccess("""
                        {"result":{"orderId":"ord-1","clientOrderId":"generated"}}""", APPLICATION_JSON));

        var resp = service.buyMarket("005930", new BigDecimal("10"));

        assertThat(resp.orderId()).isEqualTo("ord-1");
        server.verify();
    }

    @Test
    void limitOrderSendsPriceAndProvidedClientOrderId() {
        server.expect(requestTo("http://toss.test/api/v1/orders"))
                .andExpect(jsonPath("$.orderType").value("LIMIT"))
                .andExpect(jsonPath("$.price").value("70000"))
                .andExpect(jsonPath("$.timeInForce").value("DAY"))
                .andExpect(jsonPath("$.clientOrderId").value("my-key-1"))
                .andRespond(withSuccess("""
                        {"result":{"orderId":"ord-2","clientOrderId":"my-key-1"}}""", APPLICATION_JSON));

        var req = OrderCreateRequest.limit("005930", Side.SELL, new BigDecimal("5"), new BigDecimal("70000"))
                .withClientOrderId("my-key-1");
        var resp = service.place(req);

        assertThat(resp.orderId()).isEqualTo("ord-2");
        server.verify();
    }

    @Test
    void highValueOrderErrorIsMapped() {
        server.expect(requestTo("http://toss.test/api/v1/orders"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(APPLICATION_JSON)
                        .body("""
                                {"error":{"requestId":"r1","code":"confirm-high-value-required",
                                "message":"1억원 이상 주문은 확인이 필요합니다"}}"""));

        assertThatThrownBy(() -> service.buyMarket("005930", new BigDecimal("100000")))
                .isInstanceOf(TossApiException.class)
                .isNotInstanceOf(TossTransientException.class) // 400 → 재시도 대상 아님
                .satisfies(e -> assertThat(((TossApiException) e).code()).isEqualTo("confirm-high-value-required"));
        server.verify();
    }

    @Test
    void cancelSendsEmptyBodyAndReturnsNewOrderId() {
        server.expect(requestTo("http://toss.test/api/v1/orders/ord-1/cancel"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$").isMap())
                .andRespond(withSuccess("""
                        {"result":{"orderId":"ord-1-cancel"}}""", APPLICATION_JSON));

        var resp = service.cancel("ord-1");

        assertThat(resp.orderId()).isEqualTo("ord-1-cancel");
        server.verify();
    }
}
