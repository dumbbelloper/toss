package com.toss.notify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramNotifierTest {

    private static final String BASE = "https://api.telegram.org/botTEST";

    private record Fixture(TelegramNotifier notifier, MockRestServiceServer server) {
    }

    private Fixture newFixture(String chatId) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramClient client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(TelegramClient.class);
        var notifier = new TelegramNotifier(client, new TelegramProperties(true, "TEST", chatId));
        return new Fixture(notifier, server);
    }

    @Test
    void sendsChatIdAndText() {
        Fixture f = newFixture("12345");
        f.server().expect(requestTo(BASE + "/sendMessage"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.chat_id").value("12345"))
                .andExpect(jsonPath("$.text").value("hello"))
                .andRespond(withSuccess("{\"ok\":true}", APPLICATION_JSON));

        f.notifier().send("hello");

        f.server().verify();
    }

    @Test
    void swallowsTransportErrors() {
        Fixture f = newFixture("12345");
        f.server().expect(requestTo(BASE + "/sendMessage"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatCode(() -> f.notifier().send("boom")).doesNotThrowAnyException();
    }

    @Test
    void skipsWhenChatIdMissing() {
        Fixture f = newFixture(null); // 기대 요청 미설정 → 호출하면 실패. 건너뛰어야 통과.

        assertThatCode(() -> f.notifier().send("nope")).doesNotThrowAnyException();
        f.server().verify(); // 호출이 없었음을 확인
    }
}
