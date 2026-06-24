package com.toss.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 지연(deferred) CSRF 토큰을 매 요청마다 강제로 로드해 XSRF-TOKEN 쿠키가 내려가도록 한다.
 * SPA 는 첫 요청(예: GET /api/me)의 응답으로 쿠키를 받아 이후 변경 요청에 헤더로 동봉한다.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // 지연 토큰 렌더링 트리거 → 쿠키 기록
        }
        filterChain.doFilter(request, response);
    }
}
