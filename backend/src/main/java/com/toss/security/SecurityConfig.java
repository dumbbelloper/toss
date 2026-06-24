package com.toss.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * BFF 보안 구성.
 *
 * <p>웹 SPA 는 Keycloak 을 직접 호출하지 않는다. 이 백엔드가 confidential client 로서
 * OIDC Authorization Code 흐름을 중개하고(토큰은 서버 세션에 보관), 브라우저엔 httpOnly
 * 세션 쿠키만 내려간다. SPA 는 {@code /api/**} 와 {@code /oauth2/authorization/keycloak}
 * 만 사용한다.
 *
 * <p>미인증 요청은 로그인 페이지로 리다이렉트하지 않고 401 을 반환한다 — SPA(fetch)가
 * 401 을 받아 직접 로그인(full-page navigation)을 시작하기 위함이다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ClientRegistrationRepository clientRegistrations) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            // 로그인 성공 후 SPA 루트("/")로 복귀. 콜백이 Vite proxy(:5173)를 거치므로
            // 상대 경로 "/"는 브라우저 기준 :5173/ 로 해석된다.
            .oauth2Login(login -> login.defaultSuccessUrl("/", true))
            .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrations)))
            // SPA(쿠키 세션) → CSRF 방어 필수. XSRF-TOKEN 쿠키(JS 가독) + BREACH 보호.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /** RP-initiated logout: 백엔드 세션 종료 후 Keycloak 세션까지 종료하고 SPA 로 복귀. */
    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrations) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
        handler.setPostLogoutRedirectUri("http://localhost:5173/");
        return handler;
    }
}
