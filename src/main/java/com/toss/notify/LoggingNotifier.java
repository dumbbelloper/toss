package com.toss.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 텔레그램 비활성 시 사용하는 기본 알림 구현 — 로그로만 남긴다.
 */
@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotifier implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public void send(String message) {
        log.info("[notify] {}", message);
    }
}
