package com.toss.service;

import com.toss.auth.AccountHeaderInterceptor;
import com.toss.client.AccountClient;
import com.toss.client.dto.Account;
import com.toss.client.dto.Currency;
import com.toss.client.dto.HoldingsOverview;
import com.toss.config.TossProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountServiceTest {

    private MockRestServiceServer server;
    private AccountService service;

    private void newFixture(Long accountSeq) {
        TossProperties props = new TossProperties("http://toss.test", "cid", "secret", accountSeq);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://toss.test")
                .requestInterceptor(new AccountHeaderInterceptor(props));
        server = MockRestServiceServer.bindTo(builder).build();
        AccountClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(AccountClient.class);
        service = new AccountService(client);
    }

    @Test
    void accountsDoesNotSendAccountHeader() {
        newFixture(7L);
        server.expect(requestTo("http://toss.test/api/v1/accounts"))
                .andExpect(method(GET))
                .andExpect(headerDoesNotExist("X-Tossinvest-Account")) // /accounts 는 헤더 불필요
                .andRespond(withSuccess("""
                        {"result":[{"accountNo":"123-45","accountSeq":7,"accountType":"BROKERAGE"}]}""",
                        APPLICATION_JSON));

        List<Account> accounts = service.accounts();

        assertThat(accounts).singleElement().satisfies(a -> {
            assertThat(a.accountSeq()).isEqualTo(7L);
            assertThat(a.accountType()).isEqualTo("BROKERAGE");
        });
        server.verify();
    }

    @Test
    void holdingsInjectsAccountHeader() {
        newFixture(7L);
        server.expect(requestTo("http://toss.test/api/v1/holdings"))
                .andExpect(header("X-Tossinvest-Account", "7"))
                .andRespond(withSuccess("""
                        {"result":{"totalPurchaseAmount":{"krw":"1000000","usd":null},
                        "marketValue":{"amount":{"krw":"1100000","usd":null},"amountAfterCost":{"krw":"1090000","usd":null}},
                        "profitLoss":{"amount":{"krw":"100000","usd":null},"amountAfterCost":{"krw":"90000","usd":null},
                        "rate":"0.10","rateAfterCost":"0.09"},
                        "dailyProfitLoss":{"amount":{"krw":"5000","usd":null},"rate":"0.005"},
                        "items":[{"symbol":"005930","name":"삼성전자","marketCountry":"KR","currency":"KRW",
                        "quantity":"10","lastPrice":"72000","averagePurchasePrice":"70000",
                        "marketValue":{"purchaseAmount":"700000","amount":"720000","amountAfterCost":"715000"},
                        "profitLoss":{"amount":"20000","amountAfterCost":"15000","rate":"0.0285","rateAfterCost":"0.0214"},
                        "dailyProfitLoss":{"amount":"1000","rate":"0.0014"},
                        "cost":{"commission":"100","tax":null}}]}}""", APPLICATION_JSON));

        HoldingsOverview h = service.holdings();

        assertThat(h.totalPurchaseAmount().krw()).isEqualByComparingTo("1000000");
        assertThat(h.items()).singleElement().satisfies(i -> {
            assertThat(i.symbol()).isEqualTo("005930");
            assertThat(i.currency()).isEqualTo(Currency.KRW);
            assertThat(i.quantity()).isEqualByComparingTo("10");
            assertThat(i.cost().tax()).isNull();
        });
        server.verify();
    }
}
