package com.toss.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Spring Framework 7 네이티브 복원력 기능 활성화 ({@code @Retryable}, {@code @ConcurrencyLimit}).
 * 별도 라이브러리 없이 메서드 단위 재시도/동시성 제어를 사용한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {
}
