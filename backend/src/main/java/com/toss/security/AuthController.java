package com.toss.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증된 현재 사용자 정보. SPA 는 부팅 시 이 엔드포인트로 로그인 여부를 확인한다
 * (200 → 로그인됨, 401 → 로그인 필요).
 */
@RestController
public class AuthController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", user.getSubject());
        body.put("username", user.getPreferredUsername());
        body.put("email", user.getEmail());
        body.put("name", user.getFullName());
        body.put("roles", realmRoles(user));
        return body;
    }

    /** Keycloak realm_access.roles 추출 (없으면 빈 목록). */
    private Object realmRoles(OidcUser user) {
        Object realmAccess = user.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") != null) {
            return map.get("roles");
        }
        return List.of();
    }
}
