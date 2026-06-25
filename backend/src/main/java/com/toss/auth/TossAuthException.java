package com.toss.auth;

/**
 * OAuth2 액세스 토큰 발급/관리 중 발생하는 예외.
 */
public class TossAuthException extends RuntimeException {

    public TossAuthException(String message) {
        super(message);
    }

    public TossAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
