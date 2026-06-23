package com.toss.notify;

/**
 * 알림 전송 추상화. 구현체는 설정에 따라 텔레그램 또는 로깅으로 교체된다.
 * 전송 실패가 호출 측 로직을 깨뜨리지 않도록 구현체는 예외를 삼키고 로깅한다.
 */
public interface NotificationPort {

    /** 알림 메시지를 전송한다. */
    void send(String message);
}
