package com.toss.notify;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 알림 채널 점검용 엔드포인트. 활성 {@link NotificationPort} 로 테스트 메시지를 전송한다.
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final NotificationPort notifications;

    public NotifyController(NotificationPort notifications) {
        this.notifications = notifications;
    }

    @PostMapping("/test")
    public Map<String, Object> test(@RequestParam(defaultValue = "✅ toss 테스트 알림") String text) {
        notifications.send(text);
        return Map.of("sent", true, "channel", notifications.getClass().getSimpleName());
    }
}
