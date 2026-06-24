package com.toss.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 텔레그램 Bot API 클라이언트. baseUrl 은 {@code https://api.telegram.org/bot{token}} 로
 * 구성된다(토큰이 경로에 포함). 아웃바운드 전송만 사용.
 */
@HttpExchange(accept = "application/json", contentType = "application/json")
public interface TelegramClient {

    @PostExchange("/sendMessage")
    SendMessageResponse sendMessage(@RequestBody SendMessageRequest request);

    record SendMessageRequest(
            @JsonProperty("chat_id") String chatId,
            String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SendMessageResponse(boolean ok) {
    }
}
