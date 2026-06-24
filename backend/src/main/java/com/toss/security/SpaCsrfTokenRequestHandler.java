package com.toss.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * SPA(쿠키 기반) 용 CSRF 토큰 핸들러 — Spring Security 공식 권장 패턴.
 *
 * <ul>
 *   <li>쿠키로 토큰을 내릴 때는 BREACH 보호를 위해 XOR 인코딩({@link XorCsrfTokenRequestAttributeHandler}).
 *   <li>요청 검증 시 헤더(X-XSRF-TOKEN)로 들어온 값은 원문 그대로 비교({@link CsrfTokenRequestAttributeHandler}).
 * </ul>
 *
 * SPA 는 XSRF-TOKEN 쿠키 값을 읽어 X-XSRF-TOKEN 헤더로 그대로 보낸다.
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // 쿠키 렌더링은 XOR 인코딩 핸들러에 위임.
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // 헤더로 온 값(SPA)은 원문 비교, 그 외(폼 파라미터)는 XOR 디코딩.
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
