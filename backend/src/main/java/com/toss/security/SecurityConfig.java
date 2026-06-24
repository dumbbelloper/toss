package com.toss.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 두 종류의 클라이언트를 위한 두 개의 보안 체인.
 *
 * <ol>
 *   <li><b>resource server (모바일)</b> — {@code Authorization: Bearer} 헤더가 있는 요청.
 *       Keycloak 이 발급한 access token(JWT)을 검증한다. stateless, CSRF 없음.
 *   <li><b>BFF (웹)</b> — 그 외 모든 요청. OIDC 로그인 + httpOnly 세션 쿠키 + SPA CSRF.
 * </ol>
 *
 * 모바일은 native PKCE 로 토큰을 직접 받아 Bearer 로 호출하고(react-native-app-auth),
 * 웹은 토큰을 브라우저에 노출하지 않는 BFF 를 쓴다 — 같은 백엔드, 다른 신뢰 경계.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Bearer 토큰을 가진 요청만 이 체인이 처리한다(@Order(1) 로 우선). */
    @Bean
    @Order(1)
    SecurityFilterChain apiTokenChain(HttpSecurity http) throws Exception {
        RequestMatcher hasBearer = (request) -> {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            return auth != null && auth.startsWith("Bearer ");
        };
        http
            .securityMatcher(hasBearer)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /**
     * 웹 BFF 체인. SPA 는 Keycloak 을 직접 호출하지 않고 이 백엔드가 OIDC 흐름을 중개하며,
     * 브라우저엔 httpOnly 세션 쿠키만 내려간다. 미인증 요청은 401(리다이렉트 아님) — SPA(fetch)가
     * 받아 로그인을 시작한다.
     */
    @Bean
    @Order(2)
    SecurityFilterChain bffChain(HttpSecurity http,
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
