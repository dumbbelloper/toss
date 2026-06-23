package com.toss.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@code telegram.enabled=true} 일 때만 텔레그램 클라이언트를 구성한다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramConfig {

    @Bean
    TelegramClient telegramClient(TelegramProperties props) {
        if (props.botToken() == null || props.botToken().isBlank()) {
            throw new IllegalStateException("telegram.enabled=true 이지만 telegram.bot-token 이 비어 있습니다.");
        }
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + props.botToken())
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(TelegramClient.class);
    }
}
