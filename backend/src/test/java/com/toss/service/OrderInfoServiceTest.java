package com.toss.service;

import com.toss.auth.AccountHeaderInterceptor;
import com.toss.client.OrderInfoClient;
import com.toss.client.dto.Currency;
import com.toss.config.TossProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrderInfoServiceTest {

    private MockRestServiceServer server;
    private OrderInfoService service;

    @BeforeEach
    void setUp() {
        TossProperties props = new TossProperties("http://toss.test", "cid", "secret", 42L);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://toss.test")
                .requestInterceptor(new AccountHeaderInterceptor(props));
        server = MockRestServiceServer.bindTo(builder).build();
        OrderInfoClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(OrderInfoClient.class);
        service = new OrderInfoService(client);
    }

    @Test
    void buyingPowerSendsCurrencyAndAccountHeader() {
        server.expect(requestTo("http://toss.test/api/v1/buying-power?currency=KRW"))
                .andExpect(header("X-Tossinvest-Account", "42"))
                .andRespond(withSuccess("""
                        {"result":{"currency":"KRW","cashBuyingPower":"1500000"}}""", APPLICATION_JSON));

        assertThat(service.buyingPower(Currency.KRW)).isEqualByComparingTo("1500000");
        server.verify();
    }

    @Test
    void sellableQuantityParsed() {
        server.expect(requestTo("http://toss.test/api/v1/sellable-quantity?symbol=005930"))
                .andRespond(withSuccess("""
                        {"result":{"sellableQuantity":"7"}}""", APPLICATION_JSON));

        assertThat(service.sellableQuantity("005930")).isEqualByComparingTo("7");
        server.verify();
    }

    @Test
    void commissionsParsed() {
        server.expect(requestTo("http://toss.test/api/v1/commissions"))
                .andRespond(withSuccess("""
                        {"result":[{"marketCountry":"KR","commissionRate":"0.015","startDate":"2024-01-01","endDate":null},
                        {"marketCountry":"US","commissionRate":"0.07","startDate":null,"endDate":null}]}""",
                        APPLICATION_JSON));

        var commissions = service.commissions();

        assertThat(commissions).hasSize(2);
        assertThat(commissions.getFirst().commissionRate()).isEqualByComparingTo("0.015");
        assertThat(commissions.getFirst().endDate()).isNull();
        server.verify();
    }
}
