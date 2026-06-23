package com.toss.notify;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 텔레그램 알림 설정 ({@code telegram.*}).
 *
 * @param enabled  활성화 여부 (기본 false → LoggingNotifier 사용)
 * @param botToken 봇 토큰 (BotFather 발급)
 * @param chatId   메시지를 받을 대화방 ID
 */
@ConfigurationProperties("telegram")
public record TelegramProperties(
        boolean enabled,
        String botToken,
        String chatId
) {
}
