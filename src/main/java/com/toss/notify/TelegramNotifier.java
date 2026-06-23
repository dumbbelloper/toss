package com.toss.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 텔레그램으로 알림을 전송한다. 전송 실패는 삼켜서 로깅한다(알림 실패가 비즈니스 로직을 막지 않도록).
 */
@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramNotifier implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final TelegramClient client;
    private final TelegramProperties props;

    public TelegramNotifier(TelegramClient client, TelegramProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void send(String message) {
        if (props.chatId() == null || props.chatId().isBlank()) {
            log.warn("telegram.chat-id 미설정 — 알림을 건너뜁니다: {}", message);
            return;
        }
        try {
            var response = client.sendMessage(new TelegramClient.SendMessageRequest(props.chatId(), message));
            if (response == null || !response.ok()) {
                log.warn("텔레그램 전송 실패 (ok=false): {}", message);
            }
        } catch (Exception e) {
            log.warn("텔레그램 전송 예외: {} — {}", e.toString(), message);
        }
    }
}
